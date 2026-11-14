/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.conductoross.conductor.sdk.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/**
 * Finds the steps a piece of workflow code would take by reading its compiled bytecode. Nothing is
 * run, so no value is needed: every arm of an {@code if} or {@code switch} and the body of a loop
 * are all seen, and no plain Java executes. An {@code if} or {@code switch} whose arms call the
 * server is a {@link Branch}: drawn as a SWITCH with every arm, as {@code switchOn} is. Calls are followed into lambdas and into methods of the workflow's own
 * package. What cannot be known statically stays unknown: inputs are not read, a task name computed
 * at run time shows as {@code task}, and a call through an interface or into another package is not
 * followed.
 */
public final class CodeReader {

    private static final String SDK = "org/conductoross/conductor/sdk/";
    private static final String CONDUCTOR = SDK + "Conductor";
    private static final String PROMISE = SDK + "Promise";
    private static final String LAMBDA_FACTORY = "java/lang/invoke/LambdaMetafactory";

    /** What the code would do at one point. */
    public sealed interface Step {
        String site();
    }

    /** One task. */
    public record Call(Command command, String site) implements Step {}

    /** A decision with every case; {@code otherwise} is empty when there is none. */
    public record Decision(String name, Map<String, List<Step>> cases, List<Step> otherwise, String site) implements Step {}

    /** {@code async} bodies, one branch each; {@code joined} when the code waits for them at once. */
    public record Fork(List<List<Step>> branches, boolean joined, String site) implements Step {}

    /** A {@code join} or {@code get} on lanes forked earlier, with other calls in between. */
    public record Wait(String site) implements Step {}

    /** A {@code fanOut}: its items, and so its branches, exist only at run time. */
    public record FanOut(String name, String site) implements Step {}

    /**
     * Steps inside plain Java loops ({@code for}, {@code while}, {@code do}), named as the
     * execution tags them ({@code <Class>_loop_<first line>}, outermost first). Code repeats them;
     * the definition shows them grouped, as the execution does.
     */
    public record InLoops(List<String> loops, List<Step> steps, String site) implements Step {}

    /** A {@code forEach} or {@code repeatUntil}: the server repeats these steps. */
    public record Loop(String name, boolean perItem, List<Step> body, String site) implements Step {}

    /**
     * A plain Java {@code if} or {@code switch} whose arms call the server, drawn as a SWITCH. An
     * {@code if} has the arms {@code true} and {@code false} (the default case); a {@code switch}
     * has one arm per case, keyed by its labels, and {@code default}. {@code line} is the line the
     * code decides on and {@code lastLine} the last line of its arms; {@code at} is the instruction
     * that decides and {@code region} the instructions of its arms.
     */
    public record Branch(String name, int line, int lastLine, List<Arm> arms, String site, int at, java.util.BitSet region) {
        /** The arm whose calls are made from this line, or null. */
        public Arm armAt(int callLine) {
            for (Arm arm : arms) {
                if (arm.lines().contains(callLine)) {
                    return arm;
                }
            }
            return null;
        }

        /** Whether {@code other}, of the same method, lies inside one of this branch's arms. */
        public boolean contains(Branch other) {
            return other != this && region.get(other.at);
        }

        /** The arm to draw as taken when the code went past without a call: the only one that can. */
        public Arm onlyArmWithoutCalls() {
            Arm only = null;
            for (Arm arm : arms) {
                if (arm.canSkip()) {
                    if (only != null) {
                        return null;
                    }
                    only = arm;
                }
            }
            return only;
        }

        Decision decision() {
            Map<String, List<Step>> cases = new LinkedHashMap<>();
            List<Step> otherwise = List.of();
            for (Arm arm : arms) {
                if (arm.otherwise()) {
                    otherwise = arm.steps();
                } else {
                    cases.put(arm.key(), arm.steps());
                }
            }
            return new Decision(name, cases, otherwise, site);
        }
    }

    /**
     * One arm of a {@link Branch}: its case key, the lines its calls are made from (no other code
     * of the method calls from them), what it would call, whether the code can go through it
     * without a call, and the loops inside it (first and last line).
     */
    public record Arm(String key, boolean otherwise, Set<Integer> lines, List<Step> steps, boolean canSkip, List<int[]> loops) {}

    /** What following a method as it runs needs: its branches, outermost first; its loops; the lines it returns from. */
    public record Shape(List<Branch> branches, List<int[]> loops, Set<Integer> returnLines, Paths paths) {
        static final Shape NONE = new Shape(List.of(), List.of(), Set.of(), Paths.NONE);
    }

    /** A branch the code went through between two calls, and the arm it took. */
    public record Passed(Branch branch, Arm arm) {}

    /**
     * The ways through one method's instructions, to tell which ifs and switches the code must have
     * gone through between two calls it made, and how.
     */
    public static final class Paths {
        static final Paths NONE = new Paths(new int[0], new int[0][], new boolean[0], new boolean[0], new boolean[0], Map.of(), Map.of(), Map.of());

        private final int[] lineAt;
        private final int[][] succ;
        private final int[][] pred;
        private final boolean[] step;
        private final boolean[] returns;
        private final boolean[] throwsAt;
        /** Each branch by the instruction that decides, with the instructions of its condition. */
        private final Map<Integer, Branch> branches;
        private final Map<Integer, int[]> condition;
        /** The arm each instruction a decision can jump to starts, by the deciding instruction. */
        private final Map<Integer, Map<Integer, Arm>> armAt;

        Paths(int[] lineAt, int[][] succ, boolean[] step, boolean[] returns, boolean[] throwsAt, Map<Integer, Branch> branches,
                Map<Integer, int[]> condition, Map<Integer, Map<Integer, Arm>> armAt) {
            this.lineAt = lineAt;
            this.succ = succ;
            this.step = step;
            this.returns = returns;
            this.throwsAt = throwsAt;
            this.branches = branches;
            this.condition = condition;
            this.armAt = armAt;
            List<List<Integer>> p = new ArrayList<>();
            for (int i = 0; i < succ.length; i++) {
                p.add(new ArrayList<>());
            }
            for (int i = 0; i < succ.length; i++) {
                for (int t : succ[i]) {
                    if (t < succ.length) {
                        p.get(t).add(i);
                    }
                }
            }
            this.pred = new int[succ.length][];
            for (int i = 0; i < succ.length; i++) {
                pred[i] = p.get(i).stream().mapToInt(Integer::intValue).toArray();
            }
        }

        /**
         * The branches every way from the call made on {@code fromLine} (0: the start of the
         * method) to the call made on {@code toLine} (-1: a return) goes through exactly once,
         * taking one arm only, without making another call: in the order the code reaches them.
         */
        public List<Passed> passed(int fromLine, int toLine) {
            int n = succ.length;
            if (n == 0) {
                return List.of();
            }
            List<Integer> sources = new ArrayList<>();
            boolean[] target = new boolean[n];
            boolean anyStep = false;
            for (int i = 0; i < n; i++) {
                if (fromLine > 0 && step[i] && lineAt[i] == fromLine) {
                    for (int t : succ[i]) {
                        sources.add(t);
                    }
                }
                target[i] = toLine < 0 ? returns[i] : step[i] && lineAt[i] == toLine;
                anyStep |= target[i];
            }
            if (toLine > 0 && !anyStep) {
                for (int i = 0; i < n; i++) {
                    target[i] = throwsAt[i] && lineAt[i] == toLine; // the code threw there instead of calling
                }
            }
            if (fromLine <= 0) {
                sources.add(0);
            }
            // forward from the call, stopping at any other call; then back from the next call
            boolean[] fwd = new boolean[n];
            int[] depth = new int[n];
            java.util.Arrays.fill(depth, Integer.MAX_VALUE);
            ArrayDeque<Integer> todo = new ArrayDeque<>();
            boolean[] reached = new boolean[n];
            for (int s : sources) {
                if (s < n && depth[s] == Integer.MAX_VALUE) {
                    depth[s] = 0;
                    todo.add(s);
                }
            }
            while (!todo.isEmpty()) {
                int i = todo.poll();
                if (target[i]) {
                    reached[i] = true;
                    continue;
                }
                if (step[i] || fwd[i]) {
                    continue;
                }
                fwd[i] = true;
                for (int t : succ[i]) {
                    if (t < n && depth[t] == Integer.MAX_VALUE) {
                        depth[t] = depth[i] + 1;
                    }
                    if (t < n && !fwd[t]) {
                        todo.add(t);
                    }
                }
            }
            boolean[] rel = new boolean[n];
            for (int i = 0; i < n; i++) {
                if (reached[i]) {
                    todo.add(i);
                }
            }
            while (!todo.isEmpty()) {
                int i = todo.poll();
                for (int p : pred[i]) {
                    if (fwd[p] && !rel[p]) {
                        rel[p] = true;
                        todo.add(p);
                    }
                }
            }
            List<Passed> out = new ArrayList<>();
            List<Integer> order = new ArrayList<>();
            for (Map.Entry<Integer, Branch> e : branches.entrySet()) {
                int[] cond = condition.get(e.getKey());
                boolean[] in = new boolean[n];
                boolean touched = false;
                for (int i : cond) {
                    in[i] = true;
                    touched |= rel[i];
                }
                if (!touched || reaches(sources, target, rel, in) || loopsBack(cond, rel, in)) {
                    continue; // a way avoids it, or goes round to it again
                }
                Arm only = null;
                boolean unknown = false;
                for (int x : cond) {
                    for (int y : succ[x]) {
                        if (y >= n || in[y] || !(rel[y] || target[y])) {
                            continue;
                        }
                        Arm arm = armAt.get(e.getKey()).get(y);
                        if (arm == null || (only != null && only != arm)) {
                            unknown = true;
                        }
                        only = arm;
                    }
                }
                if (!unknown && only != null) {
                    out.add(new Passed(e.getValue(), only));
                    order.add(depth[cond[0]]);
                }
            }
            List<Integer> idx = new ArrayList<>();
            for (int k = 0; k < out.size(); k++) {
                idx.add(k);
            }
            idx.sort(Comparator.comparingInt(order::get));
            List<Passed> sorted = new ArrayList<>();
            idx.forEach(k -> sorted.add(out.get(k)));
            return sorted;
        }

        /**
         * Whether every way from the call on {@code fromLine} to the call on {@code toLine}, without
         * another call, goes through the decision of {@code branch}: the code decided it again.
         */
        public boolean decidesAgain(int fromLine, int toLine, Branch branch) {
            int n = succ.length;
            Integer at = null;
            for (Map.Entry<Integer, Branch> e : branches.entrySet()) {
                if (e.getValue() == branch) {
                    at = e.getKey();
                }
            }
            if (at == null || n == 0) {
                return false;
            }
            List<Integer> sources = new ArrayList<>();
            boolean[] target = callAt(toLine);
            boolean any = false;
            for (int i = 0; i < n; i++) {
                if (step[i] && lineAt[i] == fromLine) {
                    for (int t : succ[i]) {
                        sources.add(t);
                    }
                }
                any |= target[i];
            }
            if (sources.isEmpty() || !any) {
                return false;
            }
            boolean[] all = new boolean[n];
            for (int i = 0; i < n; i++) {
                all[i] = !step[i] || target[i];
            }
            int[] cond = condition.get(at);
            boolean[] in = new boolean[n];
            for (int i : cond) {
                in[i] = true;
            }
            return !reaches(sources, target, all, in) && reaches(sources, target, all, new boolean[n]);
        }

        /**
         * Whether some way from the call on {@code fromLine} to the call on {@code toLine}, without
         * another call, goes through the decision of {@code branch}: the code may have decided it
         * again in between.
         */
        public boolean mayDecideAgain(int fromLine, int toLine, Branch branch) {
            int n = succ.length;
            Integer at = null;
            for (Map.Entry<Integer, Branch> e : branches.entrySet()) {
                if (e.getValue() == branch) {
                    at = e.getKey();
                }
            }
            if (at == null || n == 0) {
                return false;
            }
            List<Integer> sources = new ArrayList<>();
            boolean[] target = callAt(toLine);
            for (int i = 0; i < n; i++) {
                if (step[i] && lineAt[i] == fromLine) {
                    for (int t : succ[i]) {
                        sources.add(t);
                    }
                }
            }
            int[] cond = condition.get(at);
            boolean[] all = new boolean[n];
            for (int i = 0; i < n; i++) {
                all[i] = !step[i] || target[i];
            }
            // from the call to the decision, and from the decision on to the next call
            boolean[] atDecision = new boolean[n];
            atDecision[cond[0]] = true;
            boolean[] noCall = new boolean[n];
            for (int i = 0; i < n; i++) {
                noCall[i] = !step[i]; // on the way to the decision, the next call is not passed either
            }
            if (!reaches(sources, atDecision, noCall, new boolean[n])) {
                return false;
            }
            boolean[] in = new boolean[n];
            for (int i : cond) {
                in[i] = true;
            }
            List<Integer> after = new ArrayList<>();
            for (int x : cond) {
                for (int y : succ[x]) {
                    if (y < n && !in[y]) {
                        after.add(y);
                    }
                }
            }
            return reaches(after, target, all, new boolean[n]);
        }

        /**
         * Whether the code can get from the call on {@code fromLine} to the call on {@code toLine}
         * without another call, staying in one invocation of the method.
         */
        public boolean reachable(int fromLine, int toLine) {
            int n = succ.length;
            List<Integer> sources = new ArrayList<>();
            boolean[] target;
            boolean[] noCall = new boolean[n];
            for (int i = 0; i < n; i++) {
                if (step[i] && lineAt[i] == fromLine) {
                    for (int t : succ[i]) {
                        sources.add(t);
                    }
                }
                noCall[i] = !step[i];
            }
            target = callAt(toLine);
            return reaches(sources, target, noCall, new boolean[n]);
        }

        /** Where a call on {@code toLine} is: its calls, or, when the code threw there instead, its throw. */
        private boolean[] callAt(int toLine) {
            int n = succ.length;
            boolean[] target = new boolean[n];
            boolean any = false;
            for (int i = 0; i < n; i++) {
                target[i] = step[i] && lineAt[i] == toLine;
                any |= target[i];
            }
            if (!any) {
                for (int i = 0; i < n; i++) {
                    target[i] = throwsAt[i] && lineAt[i] == toLine;
                }
            }
            return target;
        }

        /** Whether some way from the sources reaches a target without touching the excluded instructions. */
        private boolean reaches(List<Integer> sources, boolean[] target, boolean[] rel, boolean[] excluded) {
            boolean[] seen = new boolean[succ.length];
            ArrayDeque<Integer> todo = new ArrayDeque<>(sources);
            while (!todo.isEmpty()) {
                int i = todo.poll();
                if (i >= succ.length || excluded[i] || seen[i]) {
                    continue;
                }
                if (target[i]) {
                    return true;
                }
                if (!rel[i]) {
                    continue;
                }
                seen[i] = true;
                for (int t : succ[i]) {
                    todo.add(t);
                }
            }
            return false;
        }

        /** Whether the code can come round from a decision to the same decision without a call. */
        private boolean loopsBack(int[] cond, boolean[] rel, boolean[] in) {
            boolean[] seen = new boolean[succ.length];
            ArrayDeque<Integer> todo = new ArrayDeque<>();
            for (int x : cond) {
                for (int y : succ[x]) {
                    if (y < succ.length && !in[y]) {
                        todo.add(y);
                    }
                }
            }
            while (!todo.isEmpty()) {
                int i = todo.poll();
                if (i >= succ.length || seen[i] || !rel[i]) {
                    continue;
                }
                if (in[i]) {
                    return true;
                }
                seen[i] = true;
                for (int t : succ[i]) {
                    todo.add(t);
                }
            }
            return false;
        }
    }

    private static final Map<String, List<Step>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ClassNode> CLASSES = new ConcurrentHashMap<>();
    private static final Map<String, Shape> SHAPES = new ConcurrentHashMap<>();
    /** Where each method, or code it calls, calls Thread.sleep. */
    private static final Map<String, List<String>> SLEEPS = new ConcurrentHashMap<>();

    /**
     * Where a workflow method, or a lambda or helper it calls, calls {@code Thread.sleep}, as
     * {@code Class.method:line}. Plain Java runs again on every replay, so such a sleep is repeated
     * each time the code continues and holds a worker thread meanwhile.
     */
    public static List<String> threadSleeps(Method method) {
        String owner = Type.getInternalName(method.getDeclaringClass());
        CodeReader reader = new CodeReader(method.getDeclaringClass().getClassLoader(), owner);
        String key = reader.key(owner, method.getName(), Type.getMethodDescriptor(method));
        try {
            reader.walk(owner, method.getName(), Type.getMethodDescriptor(method), new HashSet<>());
        } catch (RuntimeException e) {
            return List.of();
        }
        return SLEEPS.getOrDefault(key, List.of());
    }

    private final ClassLoader loader;
    private final String scope;

    private CodeReader(ClassLoader loader, String rootClass) {
        this.loader = loader;
        int slash = rootClass.lastIndexOf('/');
        this.scope = slash < 0 ? "" : rootClass.substring(0, slash);
    }

    /** The steps of a method, such as a {@code @Workflow} method. */
    public static List<Step> read(Method method) {
        String owner = Type.getInternalName(method.getDeclaringClass());
        return new CodeReader(method.getDeclaringClass().getClassLoader(), owner)
                .walk(owner, method.getName(), Type.getMethodDescriptor(method), new HashSet<>());
    }

    /** The steps of a lambda or method reference, which must be serializable. */
    public static List<Step> read(Serializable body) {
        MethodRefs.Implementation impl = MethodRefs.implementation(body);
        return new CodeReader(body.getClass().getClassLoader(), impl.owner())
                .walk(impl.owner(), impl.name(), impl.descriptor(), new HashSet<>());
    }

    /**
     * What a method would call after the code on {@code line}, had it gone on: read from the
     * compiled code, nothing runs. One list per level: the rest of the innermost drawn if or
     * switch arm the line is in, then of each arm around it, then of the method. Empty when the
     * method cannot be read.
     */
    public static List<List<Step>> stepsAfter(Class<?> type, String name, String descriptor, int line) {
        Set<Integer> branchLines = new HashSet<>();
        for (Branch b : shape(type, name, descriptor).branches()) {
            branchLines.add(b.line());
        }
        String owner = Type.getInternalName(type);
        CodeReader reader = new CodeReader(type.getClassLoader(), owner);
        try {
            ClassNode node = reader.classNode(owner);
            MethodNode method = node == null ? null : find(node, name, descriptor);
            if (method == null || method.instructions.size() == 0) {
                return List.of();
            }
            Set<String> active = new HashSet<>();
            active.add(reader.key(owner, name, descriptor));
            return reader.new Reading(owner, method, active).stepsAfter(line, branchLines);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /**
     * The branches, loops and returns of one method, for following it while it runs. A method
     * that cannot be read has none: it is then drawn as the calls it makes.
     */
    public static Shape shape(Class<?> type, String name, String descriptor) {
        String owner = Type.getInternalName(type);
        CodeReader reader = new CodeReader(type.getClassLoader(), owner);
        String key = reader.key(owner, name, descriptor);
        Shape known = SHAPES.get(key);
        if (known != null) {
            return known;
        }
        try {
            reader.walk(owner, name, descriptor, new HashSet<>());
        } catch (RuntimeException e) {
            // drawing never fails a workflow
        }
        return SHAPES.computeIfAbsent(key, k -> Shape.NONE);
    }

    private String key(String owner, String name, String descriptor) {
        return System.identityHashCode(loader) + ":" + owner + "." + name + descriptor;
    }

    private List<Step> walk(String owner, String name, String descriptor, Set<String> active) {
        String key = key(owner, name, descriptor);
        if (!active.add(key)) {
            return List.of(); // recursion: its steps are already being read
        }
        try {
            List<Step> known = CACHE.get(key);
            if (known != null) {
                return known;
            }
            String declaring = owner;
            MethodNode method = null;
            while (declaring != null && inScope(declaring) && method == null) {
                ClassNode node = classNode(declaring);
                method = node == null ? null : find(node, name, descriptor);
                if (method == null) {
                    declaring = node == null ? null : node.superName;
                }
            }
            if (method == null || method.instructions.size() == 0) {
                return List.of();
            }
            Reading reading = new Reading(declaring, method, active);
            List<Step> steps = Collections.unmodifiableList(reading.steps());
            SHAPES.put(key, reading.shape());
            SLEEPS.put(key, List.copyOf(new java.util.LinkedHashSet<>(reading.sleeps)));
            CACHE.put(key, steps);
            return steps;
        } finally {
            active.remove(key);
        }
    }

    private boolean inScope(String owner) {
        int slash = owner.lastIndexOf('/');
        String pkg = slash < 0 ? "" : owner.substring(0, slash);
        return scope.isEmpty() ? pkg.isEmpty() : pkg.equals(scope) || pkg.startsWith(scope + "/");
    }

    private ClassNode classNode(String owner) {
        String key = System.identityHashCode(loader) + ":" + owner;
        ClassNode cached = CLASSES.get(key);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = loader == null ? ClassLoader.getSystemResourceAsStream(owner + ".class") : loader.getResourceAsStream(owner + ".class")) {
            if (in == null) {
                return null;
            }
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
            CLASSES.put(key, node);
            return node;
        } catch (IOException e) {
            throw new IllegalStateException("cannot read the class " + owner, e);
        }
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode m : node.methods) {
            if (m.name.equals(name) && m.desc.equals(descriptor)) {
                return m;
            }
        }
        return null;
    }

    /**
     * A decision that may be drawn: the instruction that decides, its line, one past the last
     * instruction of its arms, each arm's key, which arm is the default, the instructions of its
     * condition, the arm each place the condition goes on to starts, each arm's code and first
     * instruction, and all its instructions.
     */
    private record Candidate(int b, int line, String name, int end, List<String> keys, int otherwise,
            int[] condition, Map<Integer, Integer> exitArm, List<boolean[]> regions, List<Integer> entries, boolean[] covers) {
        int extent() {
            return end;
        }
    }

    /** One method's instructions, read in order. */
    private final class Reading {
        private final String owner;
        private final MethodNode method;
        private final Set<String> active;
        private final Frame<SourceValue>[] frames;
        private final AbstractInsnNode[] insns;
        /** Lambdas handed to the SDK: read as that call's body, or not at all (a once supplier). */
        private final Set<AbstractInsnNode> consumed = Collections.newSetFromMap(new IdentityHashMap<>());
        private final List<Step> steps = new ArrayList<>();
        private final List<List<Step>> branches = new ArrayList<>();
        /** Forks drawn without a join that a later join or get may still wait for. */
        private int unjoined;
        /** javac copies a finally block to every exit; each copy is the same step on the same line. */
        private final boolean hasFinally;
        private final Set<String> seenInFinally = new HashSet<>();
        private int line;
        /** The source line of each instruction. */
        private final int[] lineAt;
        /** Ifs and switches that may be drawn as branches, by the instruction that decides. */
        private final Map<Integer, Candidate> candidates = new HashMap<>();
        /** Instructions that call the server, or call code that does. */
        private final Set<Integer> stepAt = new HashSet<>();
        /** Branches read, by the instruction that decides. */
        private final Map<Integer, Branch> found = new TreeMap<>();
        /** The arm that starts at each instruction a branch's decision can jump to, by the instruction that decides. */
        private final Map<Integer, Map<Integer, Arm>> armStarts = new HashMap<>();
        /** The instructions of each drawn branch's condition, by the instruction that decides. */
        private final Map<Integer, int[]> conditions = new HashMap<>();
        /** Thread.sleep calls in this method and in the code it calls. */
        private final List<String> sleeps = new ArrayList<>();
        private int current;

        Reading(String owner, MethodNode method, Set<String> active) {
            this.owner = owner;
            this.method = method;
            this.active = active;
            this.insns = method.instructions.toArray();
            this.hasFinally = method.tryCatchBlocks.stream().anyMatch(b -> b.type == null);
            try {
                this.frames = new Analyzer<>(new SourceInterpreter()).analyze(owner, method);
            } catch (AnalyzerException e) {
                throw new IllegalStateException("cannot read " + owner + "." + method.name, e);
            }
            this.lineAt = new int[insns.length];
            int at = 0;
            for (int i = 0; i < insns.length; i++) {
                if (insns[i] instanceof LineNumberNode n) {
                    at = n.line;
                }
                lineAt[i] = at;
            }
            // first, where the steps are: what decisions and loops are depends on it
            consumeLambdas();
            readRange(0, insns.length);
            steps.clear();
            branches.clear();
            unjoined = 0;
            seenInFinally.clear();
            sleeps.clear();
            findCandidates();
        }

        List<Step> steps() {
            consumeLambdas();
            readRange(0, insns.length);
            flushFork(false);
            return steps;
        }

        /**
         * The steps the method takes after the code on {@code line}, had it gone on: the rest of
         * the method from the end of that line. Inside an arm of an if or switch it goes on after
         * the if or switch, not into the other arms.
         */
        List<List<Step>> stepsAfter(int line, Set<Integer> branchLines) {
            consumeLambdas();
            int at = -1;
            for (int i = 0; i < insns.length; i++) {
                if (frames[i] != null && insns[i].getOpcode() >= 0 && lineAt[i] == line) {
                    at = i;
                } else if (at >= 0 && insns[i].getOpcode() >= 0 && lineAt[i] != line) {
                    break; // the first block of code on that line
                }
            }
            if (at < 0) {
                return List.of();
            }
            List<List<Step>> segments = new ArrayList<>();
            int pos = at + 1;
            while (pos < insns.length) {
                Candidate around = null; // the innermost arm the position is in
                boolean[] arm = null;
                int armSize = Integer.MAX_VALUE;
                for (Candidate c : candidates.values()) {
                    for (boolean[] r : c.regions()) {
                        int size = 0;
                        for (boolean x : r) {
                            size += x ? 1 : 0;
                        }
                        if (r[pos] && size < armSize) {
                            around = c;
                            arm = r;
                            armSize = size;
                        }
                    }
                }
                if (around == null) {
                    readRange(pos, insns.length);
                    break;
                }
                boolean[] rest = new boolean[insns.length];
                for (int i = pos; i < insns.length; i++) {
                    rest[i] = arm[i];
                }
                readIn(rest);
                if (branchLines.contains(lineAt[around.b()])) {
                    flushFork(false);
                    segments.add(List.copyOf(steps)); // the code leaves a drawn if or switch here
                    steps.clear();
                }
                pos = around.end();
            }
            flushFork(false);
            segments.add(List.copyOf(steps));
            return segments;
        }

        private void consumeLambdas() {
            for (int i = 0; i < insns.length; i++) {
                if (frames[i] != null && insns[i] instanceof MethodInsnNode m && m.owner.startsWith(SDK) && !inScope(m.owner)) {
                    int argc = Type.getArgumentTypes(m.desc).length;
                    for (int a = 0; a < argc; a++) {
                        AbstractInsnNode source = source(i, argc, a);
                        if (source instanceof InvokeDynamicInsnNode) {
                            consumed.add(source);
                        }
                    }
                }
            }
        }

        /** Reads the instructions from {@code from} up to {@code to}, adding their steps. */
        private void readRange(int from, int to) {
            boolean[] in = new boolean[insns.length];
            for (int i = Math.max(0, from); i < Math.min(to, insns.length); i++) {
                in[i] = true;
            }
            readIn(in);
        }

        /** Reads the instructions in the set, in order, adding their steps: an if or switch in it as a whole. */
        private void readIn(boolean[] in) {
            boolean[] done = new boolean[insns.length];
            for (int i = 0; i < insns.length; i++) {
                if (!in[i] || done[i]) {
                    continue;
                }
                AbstractInsnNode insn = insns[i];
                line = lineAt[i];
                if (frames[i] == null) {
                    continue; // unreachable
                }
                current = i;
                Candidate candidate = candidates.get(i);
                if (candidate != null && within(candidate.covers(), in) && branch(candidate)) {
                    for (int k = 0; k < insns.length; k++) {
                        done[k] |= candidate.covers()[k];
                    }
                    continue;
                }
                if (insn instanceof InvokeDynamicInsnNode indy && !consumed.contains(insn) && implementation(indy) != null) {
                    // a lambda given to plain code, such as list.forEach(i -> task(...))
                    Handle h = implementation(indy);
                    add(walk(h.getOwner(), h.getName(), h.getDesc(), active));
                    sleeps.addAll(SLEEPS.getOrDefault(key(h.getOwner(), h.getName(), h.getDesc()), List.of()));
                } else if (insn instanceof MethodInsnNode m) {
                    if (CONDUCTOR.equals(m.owner) && m.getOpcode() == Opcodes.INVOKESTATIC) {
                        sdkCall(i, m);
                    } else if (PROMISE.equals(m.owner) && m.name.equals("get")) {
                        joinPoint();
                    } else if (m.name.equals("<init>") && taskType(m.owner) != null) {
                        throw new IllegalStateException(site() + " creates a " + taskType(m.owner).getSimpleName()
                                + ", so its task methods would run in the workflow on every replay: take it as a"
                                + " constructor parameter of the workflow instead, and Conductor supplies one that"
                                + " schedules the tasks");
                    } else if (m.getOpcode() != Opcodes.INVOKESTATIC && !m.name.equals("<init>") && taskMethod(m) != null) {
                        // a call on a task stub, such as tasks.reserve(order)
                        call(named(Command.Kind.SIMPLE, taskMethod(m).getAnnotation(WorkerTask.class).value()));
                    } else if ("java/lang/Thread".equals(m.owner) && m.name.equals("sleep")) {
                        sleeps.add(site());
                    } else if (!m.name.equals("<init>") && inScope(m.owner)) {
                        if (active.contains(key(m.owner, m.name, m.desc))) {
                            stepAt.add(i); // a recursive call: into code that calls the server (when any does)
                        }
                        add(walk(m.owner, m.name, m.desc, active));
                        sleeps.addAll(SLEEPS.getOrDefault(key(m.owner, m.name, m.desc), List.of()));
                    }
                }
            }
        }

        private boolean within(boolean[] part, boolean[] whole) {
            for (int k = 0; k < part.length; k++) {
                if (part[k] && !whole[k] && frames[k] != null && insns[k].getOpcode() >= 0) {
                    return false;
                }
            }
            return true;
        }

        private void add(List<Step> more) {
            if (!more.isEmpty()) {
                stepAt.add(current);
                flushFork(false);
                steps.addAll(inLoops(more));
            }
        }

        /** The steps, inside the loops of this method around the current line, if any. */
        private List<Step> inLoops(List<Step> more) {
            List<String> loops = CodeLoops.around(method, owner.substring(owner.lastIndexOf('/') + 1), line);
            return loops.isEmpty() ? more : List.of(new InLoops(loops, List.copyOf(more), site()));
        }

        /** The code waits here: for the lanes just started, or for a fork left running earlier. */
        private void joinPoint() {
            branches.removeIf(List::isEmpty);
            if (!branches.isEmpty()) {
                flushFork(true);
            } else if (unjoined > 0) {
                unjoined--;
                add(new Wait(site()));
            }
        }

        private void add(Step step) {
            add(List.of(step));
        }

        private void flushFork(boolean joined) {
            branches.removeIf(List::isEmpty);
            if (!branches.isEmpty()) {
                steps.addAll(inLoops(List.of(new Fork(List.copyOf(branches), joined, site()))));
                if (!joined) {
                    unjoined++;
                }
            }
            branches.clear();
        }

        private void call(Command command) {
            if (hasFinally && !seenInFinally.add(line + ":" + command.name)) {
                stepAt.add(current); // a copy of a finally block: drawn once, yet a call wherever it runs
                return;
            }
            add(new Call(command, site()));
        }

        private String site() {
            String simple = owner.substring(owner.lastIndexOf('/') + 1);
            return simple + "." + method.name + ":" + line;
        }

        // ------------------------------------------------------------ plain ifs and switches

        /**
         * Reads an if or switch: each arm on its own. When an arm calls the server the whole is a
         * branch, drawn with every arm; otherwise it is plain code and nothing of it is drawn.
         */
        private boolean branch(Candidate c) {
            List<List<Step>> armSteps = new ArrayList<>();
            boolean calls = false;
            for (boolean[] region : c.regions()) {
                List<Step> savedSteps = new ArrayList<>(steps);
                List<List<Step>> savedForks = new ArrayList<>(branches);
                int savedUnjoined = unjoined;
                steps.clear();
                branches.clear();
                readIn(region);
                flushFork(false);
                List<Step> arm = List.copyOf(steps);
                steps.clear();
                steps.addAll(savedSteps);
                branches.clear();
                branches.addAll(savedForks);
                unjoined = savedUnjoined;
                armSteps.add(arm);
                calls |= !arm.isEmpty();
            }
            if (!calls || c.regions().size() < 2) {
                return false; // nothing to show, or only a default: the code always goes the same way
            }
            line = c.line();
            int last = line;
            List<Arm> arms = new ArrayList<>();
            for (int k = 0; k < c.regions().size(); k++) {
                boolean[] region = c.regions().get(k);
                Set<Integer> lines = new TreeSet<>();
                for (int i = 0; i < insns.length; i++) {
                    if (region[i] && frames[i] != null && insns[i].getOpcode() >= 0) {
                        last = Math.max(last, lineAt[i]);
                        if (stepAt.contains(i) || (sitesFrom(insns[i]) && onlyIn(lineAt[i], region))) {
                            lines.add(lineAt[i]); // a call, or a throw or helper the code may fail in
                        }
                    }
                }
                arms.add(new Arm(c.keys().get(k), k == c.otherwise(), Collections.unmodifiableSet(lines), armSteps.get(k),
                        canSkip(region, c.entries().get(k)), loopsIn(region)));
            }
            java.util.BitSet region = new java.util.BitSet();
            for (boolean[] r : c.regions()) {
                for (int i = 0; i < r.length; i++) {
                    if (r[i]) {
                        region.set(i);
                    }
                }
            }
            Branch b = new Branch(c.name(), c.line(), last, List.copyOf(arms), site(), c.b(), region);
            found.put(c.b(), b);
            Map<Integer, Arm> starts = new HashMap<>();
            c.exitArm().forEach((at, k) -> starts.put(at, arms.get(k)));
            armStarts.put(c.b(), starts);
            conditions.put(c.b(), c.condition());
            flushFork(false);
            steps.addAll(inLoops(List.of(b.decision())));
            return true;
        }

        /** Whether the code can fail at the instruction: a throw, or any call. */
        private boolean sitesFrom(AbstractInsnNode insn) {
            return insn.getOpcode() == Opcodes.ATHROW || isCall(insn);
        }

        /** Whether all the code on {@code line} is in the region. */
        private boolean onlyIn(int line, boolean[] region) {
            for (int i = 0; i < insns.length; i++) {
                if (frames[i] != null && insns[i].getOpcode() >= 0 && lineAt[i] == line && !region[i]) {
                    return false;
                }
            }
            return true;
        }

        Shape shape() {
            Set<Integer> returns = new TreeSet<>();
            for (int i = 0; i < insns.length; i++) {
                int op = insns[i].getOpcode();
                if (frames[i] != null && op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
                    returns.add(lineAt[i]);
                }
            }
            List<Branch> sorted = new ArrayList<>(found.values());
            sorted.sort(Comparator.comparingInt(Branch::line).thenComparing(Comparator.comparingInt(Branch::lastLine).reversed()));
            boolean[] all = new boolean[insns.length];
            java.util.Arrays.fill(all, true);
            return new Shape(List.copyOf(sorted), loopsIn(all), Collections.unmodifiableSet(returns), paths());
        }

        /** The ways through the method's instructions, for {@link Paths#passed}. */
        private Paths paths() {
            int n = insns.length;
            int[][] succ = new int[n][];
            boolean[] step = new boolean[n];
            boolean[] returns = new boolean[n];
            boolean[] throwsAt = new boolean[n];
            for (int i = 0; i < n; i++) {
                succ[i] = frames[i] == null ? new int[0] : successors(i).stream().mapToInt(Integer::intValue).toArray();
                step[i] = stepAt.contains(i);
                int op = insns[i].getOpcode();
                returns[i] = frames[i] != null && op >= Opcodes.IRETURN && op <= Opcodes.RETURN;
                throwsAt[i] = frames[i] != null && op == Opcodes.ATHROW;
            }
            Map<Integer, int[]> condition = new HashMap<>(conditions);
            return new Paths(lineAt.clone(), succ, step, returns, throwsAt, new TreeMap<>(found), condition, new HashMap<>(armStarts));
        }

        /** Whether the code can go from {@code from} out of the range without making a call. */
        private boolean canSkip(boolean[] region, int from) {
            ArrayDeque<Integer> todo = new ArrayDeque<>(List.of(from));
            Set<Integer> seen = new HashSet<>();
            while (!todo.isEmpty()) {
                int i = todo.pop();
                if (i >= insns.length || !region[i] || exits(insns[i])) {
                    return true;
                }
                if (!seen.add(i) || stepAt.contains(i) || frames[i] == null) {
                    continue;
                }
                todo.addAll(successors(i));
            }
            return false;
        }

        /** The loops in a region, as their first and last line: a backward jump closes one. */
        private List<int[]> loopsIn(boolean[] region) {
            List<int[]> loops = new ArrayList<>();
            for (int i = 0; i < insns.length; i++) {
                if (region[i] && frames[i] != null && insns[i] instanceof JumpInsnNode j && index(j.label) <= i
                        && region[index(j.label)] && realBackEdge(i, index(j.label))) {
                    int first = Integer.MAX_VALUE;
                    int last = 0;
                    for (int k = index(j.label); k <= i; k++) {
                        if (insns[k].getOpcode() >= 0 && lineAt[k] > 0) {
                            first = Math.min(first, lineAt[k]);
                            last = Math.max(last, lineAt[k]);
                        }
                    }
                    if (last > 0) {
                        loops.add(new int[] {first, last});
                    }
                }
            }
            return List.copyOf(loops);
        }

        // ------------------------------------------------------------ control flow
        //
        // The rules, written once (design/native-workflows/README.md, "How plain Java control flow
        // is drawn"):
        //  - a loop is a natural loop: a jump back to an instruction every way to the jump passes
        //    first. A loop that goes round a step is a real loop; one that does not is plain code.
        //  - a decision is a switch, or the jumps of one condition (`a && b` is one jump per operand).
        //    Its condition is what it can come round to without a step (a failed `when` guard goes
        //    back to the switch), and its outcomes are the places the condition goes on to.
        //  - a loop's condition is its own, not a decision: the jump out of it that the straight
        //    code from its head comes to, or the jump back that closes it. Any other jump out of
        //    a loop is a break, and the if around it a decision.
        //  - an arm holds what runs only because its outcome was taken: the code every way to which
        //    goes through that outcome, without leaving the loop the decision is in.
        //  - a decision is drawn when an arm holds a step, the arms take up all the code from the
        //    decision to where they meet, and every line with a step is in one arm or in none.

        /** Every way on from each instruction, without (flow) and with (flowAll) the ways into catch handlers. */
        private int[][] flow;
        private int[][] flowAll;
        private int[][] backAll;
        /** Instructions code jumps to or a handler starts at: where a block of straight code starts. */
        private boolean[] entered;
        /** The natural loops, and which of them go round a step. */
        private final List<NaturalLoop> naturalLoops = new ArrayList<>();

        /**
         * A natural loop: its head, the code that goes round to it, the jumps back, whether it goes
         * round a step, and its extent: what runs from its head before its follow, the one place
         * every way out of it leads to (all of the code after it when there is no such place).
         */
        private record NaturalLoop(int head, boolean[] body, Set<Integer> backFrom, boolean real, boolean[] extent, int size) {
            boolean spans(int i) {
                return extent[i];
            }
        }

        private interface Edge {
            boolean ok(int from, int to);
        }

        private void buildFlow() {
            int n = insns.length;
            List<List<Integer>> plain = new ArrayList<>();
            List<List<Integer>> all = new ArrayList<>();
            List<List<Integer>> back = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                plain.add(new ArrayList<>());
                all.add(new ArrayList<>());
                back.add(new ArrayList<>());
            }
            entered = new boolean[n];
            for (int i = 0; i < n; i++) {
                if (frames[i] == null) {
                    continue;
                }
                for (int t : successors(i)) {
                    if (t < n) {
                        plain.get(i).add(t);
                        all.get(i).add(t);
                    }
                }
                for (int t : targets(i)) {
                    entered[t] = true;
                }
            }
            for (TryCatchBlockNode tc : method.tryCatchBlocks) {
                int h = index(tc.handler);
                entered[h] = true;
                for (int i = index(tc.start); i < index(tc.end); i++) {
                    if (frames[i] != null && insns[i].getOpcode() >= 0) {
                        all.get(i).add(h);
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                for (int t : all.get(i)) {
                    back.get(t).add(i);
                }
            }
            flow = toArrays(plain);
            flowAll = toArrays(all);
            backAll = toArrays(back);
        }

        private int[][] toArrays(List<List<Integer>> lists) {
            int[][] out = new int[lists.size()][];
            for (int i = 0; i < out.length; i++) {
                out[i] = lists.get(i).stream().mapToInt(Integer::intValue).toArray();
            }
            return out;
        }

        /** What the sources reach along {@code graph}, entering only instructions {@code ok} and taking only edges {@code edge}. */
        private boolean[] reach(int[][] graph, List<Integer> sources, java.util.function.IntPredicate ok, Edge edge) {
            boolean[] seen = new boolean[graph.length];
            ArrayDeque<Integer> todo = new ArrayDeque<>();
            for (int s : sources) {
                if (s < graph.length && !seen[s] && ok.test(s)) {
                    seen[s] = true;
                    todo.add(s);
                }
            }
            while (!todo.isEmpty()) {
                int i = todo.poll();
                for (int t : graph[i]) {
                    if (!seen[t] && ok.test(t) && edge.ok(i, t)) {
                        seen[t] = true;
                        todo.add(t);
                    }
                }
            }
            return seen;
        }

        /** Natural loops: a jump back to {@code h} from {@code s} where every way from the start to {@code s} passes {@code h}. */
        private void findLoops() {
            int n = insns.length;
            Map<Integer, Set<Integer>> byHead = new TreeMap<>();
            Map<Integer, boolean[]> withoutHead = new HashMap<>();
            for (int i = 0; i < n; i++) {
                for (int t : flowAll[i]) {
                    if (t <= i) {
                        int h = t;
                        boolean[] r = withoutHead.computeIfAbsent(h, x -> reach(flowAll, List.of(0), k -> k != x, (a, b) -> true));
                        if (!r[i]) {
                            byHead.computeIfAbsent(h, x -> new TreeSet<>()).add(i);
                        }
                    }
                }
            }
            for (Map.Entry<Integer, Set<Integer>> e : byHead.entrySet()) {
                int h = e.getKey();
                boolean[] body = reach(backAll, new ArrayList<>(e.getValue()), k -> k != h, (a, b) -> true);
                body[h] = true;
                boolean real = false;
                for (int i = 0; i < n; i++) {
                    real |= body[i] && stepAt.contains(i); // (what goes round: not the code breaking out)
                }
                // the ways out: where code in it goes to outside it
                Set<Integer> exits = new TreeSet<>();
                for (int i = 0; i < n; i++) {
                    if (body[i]) {
                        for (int t : flow[i]) {
                            if (!body[t]) {
                                exits.add(t);
                            }
                        }
                    }
                }
                Map<Integer, boolean[]> from = new HashMap<>();
                for (int t : exits) {
                    from.put(t, reach(flowAll, List.of(t), k -> !body[k], (a, b) -> true));
                }
                // the follow: where every way out that can go on (not only throw) leads; else the last
                // of them, where javac puts the code after the loop
                Set<Integer> goOn = new TreeSet<>();
                for (int t : exits) {
                    boolean[] r = from.get(t);
                    for (int i = 0; i < n && !goOn.contains(t); i++) {
                        int op = insns[i].getOpcode();
                        if (r[i] && op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
                            goOn.add(t);
                        }
                    }
                }
                Set<Integer> ways = goOn.isEmpty() ? exits : goOn;
                Integer follow = null;
                for (int t : ways) {
                    if (ways.stream().allMatch(u -> from.get(u)[t])) {
                        follow = t;
                    }
                }
                if (follow == null && !ways.isEmpty()) {
                    follow = Collections.max(ways);
                }
                final Integer f = follow;
                boolean[] extent = f == null ? reach(flowAll, List.of(0), k -> true, (a, b) -> true)
                        : reach(flowAll, List.of(h), k -> k != f, (a, b) -> true);
                int size = 0;
                for (boolean x : extent) {
                    size += x ? 1 : 0;
                }
                naturalLoops.add(new NaturalLoop(h, body, e.getValue(), real, extent, size));
            }
        }

        /** The innermost natural loop around {@code i} that {@code skip} does not reject, or null. */
        private NaturalLoop loopAround(int i, java.util.function.Predicate<NaturalLoop> skip) {
            NaturalLoop best = null;
            for (NaturalLoop l : naturalLoops) {
                if (l.spans(i) && !skip.test(l) && (best == null || l.size() < best.size())) {
                    best = l;
                }
            }
            return best;
        }

        private boolean realBackEdge(int from, int to) {
            for (NaturalLoop l : naturalLoops) {
                if (l.real() && l.head() == to && l.backFrom().contains(from)) {
                    return true;
                }
            }
            return false;
        }

        /** The first instruction of the straight code that ends at {@code j}: after a jump, a return, a step, or where code jumps in. */
        private int blockStart(int j) {
            int k = j;
            while (k > 0 && !entered[k]) {
                AbstractInsnNode prev = insns[k - 1];
                boolean ends = prev.getOpcode() >= 0 && (prev instanceof JumpInsnNode || prev instanceof TableSwitchInsnNode
                        || prev instanceof LookupSwitchInsnNode || exits(prev));
                if (ends || stepAt.contains(k - 1)) {
                    break;
                }
                k--;
            }
            return k;
        }

        private void findCandidates() {
            int n = insns.length;
            buildFlow();
            findLoops();
            List<Candidate> all = new ArrayList<>();
            boolean[] taken = new boolean[n];
            for (int b = 0; b < n; b++) {
                if (frames[b] != null && (insns[b] instanceof TableSwitchInsnNode || insns[b] instanceof LookupSwitchInsnNode)) {
                    Candidate c = switchCandidate(b);
                    if (c != null) {
                        all.add(c);
                        for (int k : c.condition()) {
                            taken[k] = true;
                        }
                    }
                }
            }
            // the jumps of one condition, read from its last: a jump whose fall-through starts the
            // condition after it, and whose other way goes into that condition or where it goes, is
            // one of its operands
            Map<Integer, IfGroup> byEntry = new HashMap<>();
            List<IfGroup> groups = new ArrayList<>();
            for (int j = n - 1; j >= 0; j--) {
                if (frames[j] == null || taken[j] || !(insns[j] instanceof JumpInsnNode jump) || !conditional(jump.getOpcode())) {
                    continue;
                }
                int start = blockStart(j);
                int fall = realAt(j + 1);
                int to = realAt(index(jump.label));
                IfGroup g = byEntry.get(fall);
                if (g != null && (g.nodes.contains(to) || g.exits.contains(to))) {
                    byEntry.remove(fall);
                    for (int k = start; k <= j; k++) {
                        g.nodes.add(k);
                    }
                    g.first = j;
                    byEntry.put(realAt(start), g);
                } else if (fall != to) {
                    IfGroup ng = new IfGroup(j, fall, to);
                    ng.first = j;
                    for (int k = start; k <= j; k++) {
                        ng.nodes.add(k);
                    }
                    byEntry.put(realAt(start), ng);
                    groups.add(ng);
                }
            }
            for (IfGroup g : groups) {
                NaturalLoop around = loopAround(g.last, l -> false);
                if (around != null && (!around.spans(g.then) || !around.spans(g.otherwise)) && condition(around, g)) {
                    continue; // the loop's condition (any other jump out of it is a break)
                }
                int[] cond = g.nodes.stream().mapToInt(Integer::intValue).sorted().toArray();
                Map<Integer, String> keyOf = new LinkedHashMap<>();
                keyOf.put(g.then, "true");
                keyOf.put(g.otherwise, "false");
                Candidate c = decision(g.last, lineAt[g.first], name("if", g.last), cond, keyOf, g.otherwise, Set.of());
                if (c != null) {
                    all.add(c);
                }
            }
            for (Candidate c : all) {
                candidates.put(c.b(), c);
            }
        }

        /**
         * Whether the jumps are the loop's condition: the first jump the straight code from its head
         * comes to (a while or for), or the jump back that closes it (a do-while).
         */
        private boolean condition(NaturalLoop loop, IfGroup g) {
            for (int j : g.nodes) {
                if (loop.backFrom().contains(j)) {
                    return true;
                }
            }
            for (int i = loop.head(); i < insns.length; i++) {
                if (i > loop.head() && entered[i]) {
                    return false;
                }
                AbstractInsnNode insn = insns[i];
                if (insn instanceof JumpInsnNode || insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode) {
                    return g.nodes.contains(i);
                }
                if (exits(insn)) {
                    return false;
                }
            }
            return false;
        }

        /** The jumps of one if's condition, its last, and its two outcomes. */
        private static final class IfGroup {
            final int last;
            final int then;
            final int otherwise;
            int first;
            final Set<Integer> nodes = new TreeSet<>();
            final Set<Integer> exits;

            IfGroup(int last, int then, int otherwise) {
                this.last = last;
                this.then = then;
                this.otherwise = otherwise;
                this.exits = Set.of(then, otherwise);
            }
        }

        /**
         * A switch: its condition is the switch and what it comes round to without a step (a
         * failed {@code when} guard goes back to it); each place the condition goes on to is an
         * outcome, named by the cases that lead there.
         */
        private Candidate switchCandidate(int b) {
            int n = insns.length;
            List<Integer> keys = new ArrayList<>();
            List<LabelNode> labels;
            LabelNode dflt;
            if (insns[b] instanceof TableSwitchInsnNode t) {
                for (int k = t.min; k <= t.max; k++) {
                    keys.add(k);
                }
                labels = t.labels;
                dflt = t.dflt;
            } else {
                LookupSwitchInsnNode l = (LookupSwitchInsnNode) insns[b];
                keys.addAll(l.keys);
                labels = l.labels;
                dflt = l.dflt;
            }
            java.util.function.IntPredicate free = k -> !stepAt.contains(k);
            Edge forward = (a, t) -> !realBackEdge(a, t);
            boolean[] from = reach(flow, List.of(b), free, forward);
            int[][] rev = new int[n][];
            List<List<Integer>> r = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                r.add(new ArrayList<>());
            }
            for (int i = 0; i < n; i++) {
                for (int t : flow[i]) {
                    r.get(t).add(i);
                }
            }
            rev = toArrays(r);
            boolean[] to = reach(rev, List.of(b), free, (a, t) -> !realBackEdge(t, a));
            Set<Integer> cond = new TreeSet<>();
            for (int i = 0; i < n; i++) {
                if (from[i] && to[i]) {
                    cond.add(i);
                }
            }
            for (int k = blockStart(b); k <= b; k++) {
                cond.add(k);
            }
            Map<Integer, String> names = caseNames(b);
            boolean chars = isChar(source(b, 1, 0));
            int d = realAt(index(dflt));
            Map<Integer, List<String>> byExit = new LinkedHashMap<>();
            for (int k = 0; k < keys.size(); k++) {
                int start = index(labels.get(k));
                String name = names.getOrDefault(keys.get(k), chars ? String.valueOf((char) keys.get(k).intValue()) : String.valueOf(keys.get(k)));
                if (!cond.contains(start)) {
                    if (realAt(start) != d) {
                        byExit.computeIfAbsent(realAt(start), x -> new ArrayList<>()).add(name);
                    }
                    continue;
                }
                // a guarded case: where its guard lets the code on to
                boolean[] inGuard = reach(flow, List.of(start), i -> cond.contains(i) && i != b, (x, y) -> true);
                for (int i = 0; i < n; i++) {
                    if (inGuard[i]) {
                        for (int y : flow[i]) {
                            if (!cond.contains(y) && realAt(y) != d) {
                                byExit.computeIfAbsent(realAt(y), x -> new ArrayList<>()).add(name + " when");
                            }
                        }
                    }
                }
            }
            if (cond.contains(d)) {
                return null;
            }
            Map<Integer, String> keyOf = new LinkedHashMap<>();
            Set<String> used = new HashSet<>();
            used.add("default");
            for (Map.Entry<Integer, List<String>> e : new TreeMap<>(byExit).entrySet()) {
                String key = String.join(",", new java.util.LinkedHashSet<>(e.getValue()));
                for (int k = 2; !used.add(key); k++) {
                    key = String.join(",", new java.util.LinkedHashSet<>(e.getValue())) + " (" + k + ")";
                }
                keyOf.put(e.getKey(), key);
            }
            keyOf.put(d, "default");
            // every place the condition goes on to is an outcome
            for (int c : cond) {
                for (int y : flow[c]) {
                    if (!cond.contains(y) && !keyOf.containsKey(realAt(y))) {
                        return null;
                    }
                }
            }
            Set<Integer> cases = new HashSet<>(keyOf.keySet());
            cases.remove(d);
            return decision(b, lineAt[b], name("switch", b), cond.stream().mapToInt(Integer::intValue).toArray(), keyOf, d, cases);
        }

        private boolean regionHasCode(boolean[] region) {
            for (int i = 0; i < region.length; i++) {
                if (region[i]) {
                    return true;
                }
            }
            return false;
        }

        /** Whether the value at {@code src} is a char: so a switch's cases are characters. */
        private boolean isChar(AbstractInsnNode src) {
            if (src instanceof MethodInsnNode m) {
                return m.desc.endsWith(")C");
            }
            if (src instanceof FieldInsnNode f) {
                return f.desc.equals("C");
            }
            if (src instanceof InsnNode i) {
                return i.getOpcode() == Opcodes.I2C || i.getOpcode() == Opcodes.CALOAD;
            }
            if (src instanceof VarInsnNode v && v.getOpcode() == Opcodes.ILOAD && method.localVariables != null) {
                int at = indexOf(v);
                for (LocalVariableNode local : method.localVariables) {
                    if (local.index == v.var && index(local.start) <= at + 1 && at < index(local.end)) {
                        return local.desc.equals("C");
                    }
                }
            }
            return false;
        }

        /**
         * A decision at {@code b} with its condition and outcomes (by the first instruction each
         * goes to, in the order to draw, {@code otherwise} the default): its arms, or null when it
         * is not drawn.
         */
        private Candidate decision(int b, int line, String name, int[] cond, Map<Integer, String> keyOf, int otherwise,
                Set<Integer> cases) {
            int n = insns.length;
            boolean[] inCond = new boolean[n];
            for (int c : cond) {
                inCond[c] = true;
            }
            // the loop the decision is in (not one that is only its own condition going round)
            NaturalLoop loop = loopAround(b, l -> {
                for (int i = 0; i < n; i++) {
                    if (l.body()[i] && !inCond[i] && insns[i].getOpcode() >= 0) {
                        return false;
                    }
                }
                return true;
            });
            List<Integer> exits = new ArrayList<>(keyOf.keySet());
            exits.sort(null);
            List<boolean[]> regions = new ArrayList<>();
            List<Integer> entries = new ArrayList<>();
            List<String> keys = new ArrayList<>();
            int[] place = new int[n];
            java.util.Arrays.fill(place, -1);
            Map<Integer, Integer> exitArm = new HashMap<>();
            boolean steps = false;
            int end = cond[cond.length - 1] + 1;
            int otherwiseAt = -1;
            for (int x : exits) {
                List<Integer> starts = new ArrayList<>();
                for (int c : cond) {
                    for (int y : flow[c]) {
                        if (!inCond[y] && realAt(y) == x) {
                            starts.add(y);
                        }
                    }
                }
                Set<Integer> startSet = new HashSet<>(starts);
                boolean[] around = reach(flowAll, List.of(0), k -> true, (a, t) -> !(inCond[a] && startSet.contains(t)));
                boolean[] region = reach(flowAll, starts, k -> !around[k] && !inCond[k] && (loop == null || loop.spans(k)), (a, t) -> true);
                int k = regions.size();
                for (int i = 0; i < n; i++) {
                    if (region[i]) {
                        if (place[i] >= 0) {
                            return null; // (cannot happen: what runs only through one outcome is in no other)
                        }
                        place[i] = k;
                        end = Math.max(end, i + 1); // (an empty arm is where the code goes on, not part of it)
                        steps |= stepAt.contains(i);
                    }
                }
                regions.add(region);
                entries.add(starts.isEmpty() ? x : Collections.min(starts));
                for (int s : starts) {
                    exitArm.put(s, k);
                }
                exitArm.put(x, k);
                if (x == otherwise) {
                    otherwiseAt = k;
                }
                keys.add(keyOf.get(x));
            }
            if (!steps || regions.size() < 2) {
                return null; // no arm calls the server: plain code
            }
            // a case another arm runs into, with a step before where all the arms meet: the case
            // falls through, and its code runs for two cases (javac's code is the same as a
            // switch followed by an if, so only the written case tells them apart)
            boolean[] lim = new boolean[n];
            for (int i = 0; i < n; i++) {
                lim[i] = !inCond[i] && (loop == null || (loop.spans(i) && i != loop.head()));
            }
            boolean[] common = null;
            Map<Integer, List<Integer>> startsOf = new HashMap<>();
            for (int c : cond) {
                for (int y : flow[c]) {
                    if (!inCond[y]) {
                        startsOf.computeIfAbsent(realAt(y), z -> new ArrayList<>()).add(y);
                    }
                }
            }
            for (List<Integer> st : startsOf.values()) {
                boolean[] r = reach(flowAll, st, k -> lim[k], (a, t) -> true);
                if (common == null) {
                    common = r;
                } else {
                    for (int i = 0; i < n; i++) {
                        common[i] &= r[i];
                    }
                }
            }
            for (int x : cases) {
                List<Integer> st = startsOf.getOrDefault(x, List.of());
                Integer k = exitArm.get(x);
                if (st.isEmpty() || k == null || regionHasCode(regions.get(k))) {
                    continue; // only through its own case: no other arm runs into it
                }
                final boolean[] meet = common;
                boolean[] own = reach(flowAll, st, i -> lim[i] && (meet == null || !meet[i]), (a, t) -> true);
                for (int i = 0; i < n; i++) {
                    if (own[i] && stepAt.contains(i)) {
                        return null;
                    }
                }
            }
            // every line with a step is in one arm or in none, so a call tells the arm it was made in
            Map<Integer, Integer> placeOfLine = new HashMap<>();
            for (int i : stepAt) {
                Integer seen = placeOfLine.putIfAbsent(lineAt[i], place[i]);
                if (seen != null && seen != place[i]) {
                    return null;
                }
            }
            for (TryCatchBlockNode tc : method.tryCatchBlocks) {
                int h = index(tc.handler);
                for (boolean[] r : regions) {
                    if (r[h]) {
                        for (int i = index(tc.start); i < index(tc.end); i++) {
                            if (!r[i] && frames[i] != null && insns[i].getOpcode() >= 0) {
                                return null; // a catch in the arm of a try around more than the arm
                            }
                        }
                    }
                }
            }
            boolean[] covers = inCond.clone();
            for (boolean[] r : regions) {
                for (int i = 0; i < n; i++) {
                    covers[i] |= r[i];
                }
            }
            return new Candidate(b, line, name, end, List.copyOf(keys), otherwiseAt, cond, Map.copyOf(exitArm),
                    List.copyOf(regions), List.copyOf(entries), covers);
        }

        /** The first real instruction at or after {@code i} (a label comes before its line number). */
        private int realAt(int i) {
            while (i < insns.length - 1 && insns[i].getOpcode() < 0) {
                i++;
            }
            return i;
        }

        /** The case labels of a switch by key: string and enum cases as written, others as numbers. */
        private Map<Integer, String> caseNames(int b) {
            Map<Integer, String> names = new HashMap<>();
            AbstractInsnNode source = source(b, 1, 0);
            if (source instanceof MethodInsnNode m && m.name.equals("ordinal") && m.desc.equals("()I")) {
                List<String> constants = enumConstants(m.owner);
                for (int k = 0; k < constants.size(); k++) {
                    names.put(k, constants.get(k));
                }
            } else if (source instanceof InsnNode n && n.getOpcode() == Opcodes.IALOAD
                    && source(indexOf(n), 2, 0) instanceof FieldInsnNode map && map.name.startsWith("$SwitchMap$")) {
                names.putAll(switchMap(map));
            } else if (source instanceof VarInsnNode v && v.getOpcode() == Opcodes.ILOAD) {
                // a string switch: javac numbers the strings, matched by equals, into a local; the
                // local is set to -1 first, and every string switch of the method may reuse it
                int from = b - 1;
                while (from > 0 && !(insns[from] instanceof VarInsnNode init && init.getOpcode() == Opcodes.ISTORE
                        && init.var == v.var && "-1".equals(constant(from, 1, 0)))) {
                    from--;
                }
                for (int i = from + 1; i < b; i++) {
                    if (frames[i] != null && insns[i] instanceof VarInsnNode store && store.getOpcode() == Opcodes.ISTORE
                            && store.var == v.var) {
                        String k = constant(i, 1, 0);
                        for (int j = i - 1; j >= Math.max(0, i - 8) && k != null; j--) {
                            if (insns[j] instanceof LdcInsnNode ldc && ldc.cst instanceof String text) {
                                names.put(Integer.parseInt(k), text);
                                break;
                            }
                        }
                    }
                }
            } else if (source instanceof InvokeDynamicInsnNode indy && indy.bsm.getOwner().equals("java/lang/runtime/SwitchBootstraps")) {
                for (int k = 0; k < indy.bsmArgs.length; k++) {
                    Object arg = indy.bsmArgs[k];
                    String text = arg instanceof Type type ? simpleName(type.getClassName()) : String.valueOf(arg);
                    names.put(k, text);
                }
                names.put(-1, "null");
            }
            return names;
        }

        private List<String> enumConstants(String enumOwner) {
            ClassNode node = classNode(enumOwner);
            List<String> constants = new ArrayList<>();
            if (node != null) {
                for (FieldNode f : node.fields) {
                    if ((f.access & Opcodes.ACC_ENUM) != 0) {
                        constants.add(f.name);
                    }
                }
            }
            return constants;
        }

        /** javac's table from an enum's ordinal to a case number, read from where it is filled in. */
        private Map<Integer, String> switchMap(FieldInsnNode map) {
            Map<Integer, String> names = new HashMap<>();
            ClassNode holder = classNode(map.owner);
            MethodNode init = holder == null ? null : find(holder, "<clinit>", "()V");
            if (init == null) {
                return names;
            }
            AbstractInsnNode[] code = init.instructions.toArray();
            for (int i = 0; i < code.length; i++) {
                if (code[i].getOpcode() == Opcodes.IASTORE) {
                    AbstractInsnNode value = prevRealIn(code, i);
                    AbstractInsnNode ordinal = prevRealIn(code, indexIn(code, value));
                    AbstractInsnNode constant = ordinal == null ? null : prevRealIn(code, indexIn(code, ordinal));
                    Integer k = intValue(value);
                    if (k != null && constant instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETSTATIC) {
                        names.put(k, f.name);
                    }
                }
            }
            return names;
        }

        private AbstractInsnNode prevRealIn(AbstractInsnNode[] code, int i) {
            for (int j = i - 1; j >= 0; j--) {
                if (code[j].getOpcode() >= 0) {
                    return code[j];
                }
            }
            return null;
        }

        private int indexIn(AbstractInsnNode[] code, AbstractInsnNode insn) {
            for (int j = 0; j < code.length; j++) {
                if (code[j] == insn) {
                    return j;
                }
            }
            return 0;
        }

        private Integer intValue(AbstractInsnNode insn) {
            if (insn instanceof InsnNode n && n.getOpcode() >= Opcodes.ICONST_M1 && n.getOpcode() <= Opcodes.ICONST_5) {
                return n.getOpcode() - Opcodes.ICONST_0;
            }
            if (insn instanceof IntInsnNode n && n.getOpcode() != Opcodes.NEWARRAY) {
                return n.operand;
            }
            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer k) {
                return k;
            }
            return null;
        }

        private static final Set<String> PLUMBING = Set.of("equals", "hashCode", "ordinal", "booleanValue", "intValue",
                "longValue", "doubleValue", "floatValue", "shortValue", "byteValue", "charValue", "valueOf",
                "getClass", "toString", "name", "compareTo", "requireNonNull");

        /**
         * A branch is named after what it decides on: the last method its condition calls, else a
         * field it reads, else a local variable, as in {@code if_flagged} or {@code switch_kind}.
         */
        private String name(String kind, int b) {
            int s = b;
            while (s > 0 && !(insns[s] instanceof LineNumberNode)) {
                s--;
            }
            for (int i = b - 1; i >= s; i--) {
                if (insns[i] instanceof MethodInsnNode m && !PLUMBING.contains(m.name) && !m.name.startsWith("<")
                        && !m.name.startsWith("lambda$")) {
                    return kind + "_" + clean(m.name);
                }
            }
            for (int i = b - 1; i >= s; i--) {
                if (insns[i] instanceof FieldInsnNode f && !f.name.startsWith("$")) {
                    return kind + "_" + clean(f.name);
                }
            }
            for (int i = s; i < b; i++) {
                if (insns[i] instanceof VarInsnNode v && v.getOpcode() >= Opcodes.ILOAD && v.getOpcode() <= Opcodes.ALOAD) {
                    String local = localName(v.var, i);
                    if (local != null && !local.equals("this")) {
                        return kind + "_" + clean(local);
                    }
                }
            }
            return kind;
        }

        private String localName(int var, int at) {
            if (method.localVariables == null) {
                return null;
            }
            String any = null;
            for (LocalVariableNode local : method.localVariables) {
                if (local.index == var) {
                    if (index(local.start) <= at + 1 && at < index(local.end)) {
                        return local.name;
                    }
                    any = local.name;
                }
            }
            return any;
        }

        private String clean(String name) {
            return name.replaceAll("[^A-Za-z0-9_]", "_");
        }

        private String simpleName(String className) {
            return className.substring(Math.max(className.lastIndexOf('.'), className.lastIndexOf('$')) + 1);
        }

        private int index(LabelNode label) {
            return method.instructions.indexOf(label);
        }

        private List<Integer> targets(int i) {
            List<Integer> out = new ArrayList<>();
            if (insns[i] instanceof JumpInsnNode j) {
                out.add(index(j.label));
            } else if (insns[i] instanceof TableSwitchInsnNode t) {
                out.add(index(t.dflt));
                t.labels.forEach(l -> out.add(index(l)));
            } else if (insns[i] instanceof LookupSwitchInsnNode l) {
                out.add(index(l.dflt));
                l.labels.forEach(x -> out.add(index(x)));
            }
            return out;
        }

        private List<Integer> successors(int i) {
            List<Integer> out = targets(i);
            boolean jumpsAlways = insns[i].getOpcode() == Opcodes.GOTO || insns[i] instanceof TableSwitchInsnNode
                    || insns[i] instanceof LookupSwitchInsnNode;
            if (!jumpsAlways && !exits(insns[i])) {
                out.add(i + 1);
            }
            return out;
        }

        private boolean exits(AbstractInsnNode insn) {
            int op = insn.getOpcode();
            return (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) || op == Opcodes.ATHROW;
        }

        private boolean conditional(int opcode) {
            return (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE) || opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL;
        }

        private boolean isCall(AbstractInsnNode insn) {
            return insn instanceof MethodInsnNode || insn instanceof InvokeDynamicInsnNode;
        }

        private void sdkCall(int i, MethodInsnNode m) {
            int argc = Type.getArgumentTypes(m.desc).length;
            switch (m.name) {
                case "task" -> {
                    if (m.desc.startsWith("(Ljava/lang/String;")) {
                        Command c = named(Command.Kind.SIMPLE, text(i, argc, 0, "task"));
                        chainArg(i, "ref").ifPresent(r -> c.explicitRef = r);
                        call(c);
                    } else {
                        Method target = target(i, argc, 0);
                        WorkerTask annotation = target == null ? null : target.getAnnotation(WorkerTask.class);
                        call(named(Command.Kind.SIMPLE, annotation == null ? "task" : annotation.value()));
                    }
                }
                case "sleep" -> call(named(Command.Kind.WAIT, "sleep"));
                case "sleepUntil" -> call(named(Command.Kind.WAIT, "sleep_until"));
                case "awaitCondition" -> call(named(Command.Kind.WAIT, "condition"));
                case "awaitExternal" -> call(step(Command.Kind.WAIT, text(i, argc, 0, "external")));
                case "human" -> call(withDefinition(step(Command.Kind.HUMAN, text(i, argc, 0, "human")), 0));
                case "llm" -> call(withDefinition(step(Command.Kind.LLM, text(i, argc, 0, "llm")), 3));
                case "agent" -> call(withDefinition(step(Command.Kind.AGENT, text(i, argc, 0, "agent")), 3));
                case "mcp" -> {
                    boolean lists = chain(i).stream().anyMatch(c -> c.name.equals("listTools"));
                    call(withDefinition(step(lists ? Command.Kind.MCP_LIST : Command.Kind.MCP_CALL, text(i, argc, 0, "mcp")), 3));
                }
                case "http" -> {
                    Command c = withDefinition(step(Command.Kind.HTTP, text(i, argc, 0, "http")), 3);
                    Map<String, Object> request = new LinkedHashMap<>();
                    request.put("method", "GET");
                    for (MethodInsnNode link : chain(i)) {
                        if (Set.of("get", "post", "put", "delete").contains(link.name)) {
                            request.put("method", link.name.toUpperCase(java.util.Locale.ROOT));
                            String uri = textOf(indexOf(link), 1, 0);
                            if (uri != null) {
                                request.put("uri", uri);
                            }
                        }
                    }
                    c.input.put("http_request", request);
                    call(c);
                }
                case "event" -> {
                    Command c = step(Command.Kind.EVENT, text(i, argc, 0, "event"));
                    c.fields.put("sink", chainArg(i, "to").orElse("conductor"));
                    call(c);
                }
                case "child" -> {
                    Command c = workflowCommand(target(i, argc, 0), Command.Kind.SUB_WORKFLOW, "");
                    if (c != null) {
                        WorkflowSpec spec = WorkflowSpec.of(target(i, argc, 0));
                        c.fields.put("subWorkflowParam", Map.of("name", spec.name, "version", spec.version));
                        call(c);
                    }
                }
                case "startWorkflow" -> {
                    String name;
                    int version = 1;
                    if (m.desc.startsWith("(Ljava/lang/String;")) {
                        name = text(i, argc, 0, "workflow");
                    } else {
                        Method target = target(i, argc, 0);
                        if (target == null) {
                            return;
                        }
                        WorkflowSpec spec = WorkflowSpec.of(target);
                        name = spec.name;
                        version = spec.version;
                    }
                    Command c = named(Command.Kind.START_WORKFLOW, "start_" + name);
                    Map<String, Object> start = new LinkedHashMap<>();
                    start.put("name", name);
                    start.put("version", version);
                    c.input.put("startWorkflow", start);
                    call(c);
                }
                case "async" -> {
                    List<Step> body = bodyOf(i, argc, 0);
                    if (!body.isEmpty()) {
                        stepAt.add(current);
                    }
                    branches.add(body);
                }
                case "join" -> joinPoint();
                case "fanOut" -> add(new FanOut(m.desc.startsWith("(Ljava/lang/String;") ? text(i, argc, 0, "fanout") : "fanout", site()));
                case "forEach" -> {
                    boolean named = m.desc.startsWith("(Ljava/lang/String;");
                    Method target = target(i, argc, argc - 1);
                    WorkerTask annotation = target == null ? null : target.getAnnotation(WorkerTask.class);
                    Step inner = new Call(named(Command.Kind.SIMPLE, annotation == null ? "task" : annotation.value()), site());
                    add(new Loop(named ? text(i, argc, 0, "for_each") : "for_each", true, List.of(inner), site()));
                }
                case "repeatUntil" -> add(new Loop("poll", false, bodyOf(i, argc, 0), site()));
                case "switchOn" -> switchOn(i, argc);
                default -> {
                    // once, now, random, workflowId: recorded values, not steps
                }
            }
        }

        private void switchOn(int i, int argc) {
            Map<String, List<Step>> cases = new LinkedHashMap<>();
            List<Step> otherwise = List.of();
            int unnamed = 0;
            for (MethodInsnNode link : chain(i)) {
                int at = indexOf(link);
                int links = Type.getArgumentTypes(link.desc).length;
                if (link.name.equals("when")) {
                    String key = constant(at, links, 0);
                    cases.put(key != null ? key : "unknown_" + ++unnamed, bodyOf(at, links, 1));
                } else if (link.name.equals("otherwise")) {
                    otherwise = bodyOf(at, links, 0);
                }
            }
            add(new Decision(text(i, argc, 0, "switch"), cases, otherwise, site()));
        }

        private Command named(Command.Kind kind, String name) {
            return new Command(kind, name, name);
        }

        /** A server step, whose first argument is its reference name. */
        private Command step(Command.Kind kind, String ref) {
            Command c = named(kind, ref);
            c.explicitRef = ref;
            return c;
        }

        private Command withDefinition(Command c, int retries) {
            c.fields.put("taskDefinition", Entries.retryingDefinition(c.name, retries, 1));
            return c;
        }

        private Command workflowCommand(Method target, Command.Kind kind, String prefix) {
            return target == null ? null : named(kind, prefix + WorkflowSpec.of(target).name);
        }

        private List<Step> bodyOf(int i, int argc, int arg) {
            if (source(i, argc, arg) instanceof InvokeDynamicInsnNode indy && implementation(indy) != null) {
                Handle h = implementation(indy);
                List<Step> body = walk(h.getOwner(), h.getName(), h.getDesc(), active);
                sleeps.addAll(SLEEPS.getOrDefault(key(h.getOwner(), h.getName(), h.getDesc()), List.of()));
                return body;
            }
            return List.of();
        }

        /** The {@code @WorkerTask} method an instance call names, or null. */
        private Method taskMethod(MethodInsnNode m) {
            if (taskType(m.owner) == null) {
                return null;
            }
            int tag = m.getOpcode() == Opcodes.INVOKEINTERFACE ? Opcodes.H_INVOKEINTERFACE : Opcodes.H_INVOKEVIRTUAL;
            Method method = MethodRefs.method(loader, new Handle(tag, m.owner, m.name, m.desc, m.itf));
            return method != null && method.isAnnotationPresent(WorkerTask.class) ? method : null;
        }

        /** The class an owner names when it has {@code @WorkerTask} methods, or null. */
        private Class<?> taskType(String owner) {
            if (owner.startsWith("java/") || owner.startsWith("javax/") || owner.startsWith("jdk/")
                    || owner.startsWith("sun/") || owner.startsWith("[") || (owner.startsWith(SDK) && !inScope(owner))) {
                return null;
            }
            try {
                Class<?> type = Class.forName(owner.replace('/', '.'), false, loader);
                return TaskStubs.isTaskType(type) ? type : null;
            } catch (ClassNotFoundException | LinkageError e) {
                return null;
            }
        }

        private Method target(int i, int argc, int arg) {
            if (source(i, argc, arg) instanceof InvokeDynamicInsnNode indy && implementation(indy) != null) {
                return MethodRefs.method(loader, implementation(indy));
            }
            return null;
        }

        private Handle implementation(InvokeDynamicInsnNode indy) {
            if (LAMBDA_FACTORY.equals(indy.bsm.getOwner()) && indy.bsmArgs.length > 1 && indy.bsmArgs[1] instanceof Handle h) {
                return h;
            }
            return null;
        }

        private int indexOf(AbstractInsnNode insn) {
            return method.instructions.indexOf(insn);
        }

        /** The instruction that produced an argument, when exactly one did. */
        private AbstractInsnNode source(int i, int argc, int arg) {
            Frame<SourceValue> frame = frames[i];
            int at = frame.getStackSize() - argc + arg;
            if (at < 0) {
                return null;
            }
            Set<AbstractInsnNode> sources = frame.getStack(at).insns;
            return sources.size() == 1 ? sources.iterator().next() : null;
        }

        private String text(int i, int argc, int arg, String otherwise) {
            String text = textOf(i, argc, arg);
            return text != null ? text : otherwise;
        }

        private String textOf(int i, int argc, int arg) {
            return source(i, argc, arg) instanceof LdcInsnNode ldc && ldc.cst instanceof String s ? s : null;
        }

        /** A compile-time value in its string form: text, a number, a boolean or an enum constant. */
        private String constant(int i, int argc, int arg) {
            AbstractInsnNode source = source(i, argc, arg);
            if (source instanceof LdcInsnNode ldc) {
                return String.valueOf(ldc.cst);
            }
            if (source instanceof IntInsnNode n) {
                return String.valueOf(n.operand);
            }
            if (source instanceof InsnNode n && n.getOpcode() >= Opcodes.ICONST_M1 && n.getOpcode() <= Opcodes.ICONST_5) {
                return String.valueOf(n.getOpcode() - Opcodes.ICONST_0);
            }
            if (source instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETSTATIC) {
                return f.name;
            }
            if (source instanceof MethodInsnNode m && m.name.equals("valueOf") && m.getOpcode() == Opcodes.INVOKESTATIC) {
                String boxed = constant(indexOf(m), Type.getArgumentTypes(m.desc).length, 0);
                if ("java/lang/Boolean".equals(m.owner) && boxed != null) {
                    return String.valueOf(!"0".equals(boxed));
                }
                return boxed;
            }
            return null;
        }

        /** The builder calls made on the result of the call at {@code i}, in order. */
        private List<MethodInsnNode> chain(int i) {
            List<MethodInsnNode> links = new ArrayList<>();
            AbstractInsnNode current = insns[i];
            for (int j = i + 1; j < insns.length; j++) {
                if (frames[j] != null && insns[j] instanceof MethodInsnNode m && m.getOpcode() != Opcodes.INVOKESTATIC) {
                    int argc = Type.getArgumentTypes(m.desc).length;
                    int at = frames[j].getStackSize() - argc - 1;
                    if (at >= 0) {
                        Set<AbstractInsnNode> receiver = frames[j].getStack(at).insns;
                        if (receiver.size() == 1 && receiver.contains(current)) {
                            links.add(m);
                            current = m;
                        }
                    }
                }
            }
            return links;
        }

        private java.util.Optional<String> chainArg(int i, String name) {
            for (MethodInsnNode link : chain(i)) {
                if (link.name.equals(name)) {
                    return java.util.Optional.ofNullable(textOf(indexOf(link), Type.getArgumentTypes(link.desc).length, 0));
                }
            }
            return java.util.Optional.empty();
        }
    }
}
