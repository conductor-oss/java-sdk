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
import java.util.ArrayList;
import java.util.ArrayDeque;
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
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.FieldNode;
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

    /** A {@code forEach} or {@code repeatUntil}: the server repeats these steps. */
    public record Loop(String name, boolean perItem, List<Step> body, String site) implements Step {}

    /**
     * A plain Java {@code if} or {@code switch} whose arms call the server, drawn as a SWITCH. An
     * {@code if} has the arms {@code true} and {@code false} (the default case); a {@code switch}
     * has one arm per case, keyed by its labels, and {@code default}. {@code line} is the line the
     * code decides on and {@code lastLine} the last line of its arms.
     */
    public record Branch(String name, int line, int lastLine, List<Arm> arms, String site) {
        /** The arm whose calls are made from this line, or null. */
        public Arm armAt(int callLine) {
            for (Arm arm : arms) {
                if (arm.lines().contains(callLine)) {
                    return arm;
                }
            }
            return null;
        }

        /** Whether {@code other} lies inside one of this branch's arms. */
        public boolean contains(Branch other) {
            return line < other.line && other.lastLine <= lastLine;
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
    public record Shape(List<Branch> branches, List<int[]> loops, Set<Integer> returnLines) {
        static final Shape NONE = new Shape(List.of(), List.of(), Set.of());
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
     * An if or switch read from the instructions: the instruction that decides, the arms as
     * instruction ranges with their keys, which one is the default, and where the arms meet.
     */
    private record Candidate(int b, String name, int end, List<int[]> ranges, List<String> keys, int otherwise) {}

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
                int armEnd = 0;
                int armSize = Integer.MAX_VALUE;
                for (Candidate c : candidates.values()) {
                    for (int[] r : c.ranges()) {
                        if (r[0] <= pos && pos < r[1] && r[1] - r[0] < armSize) {
                            around = c;
                            armEnd = r[1];
                            armSize = r[1] - r[0];
                        }
                    }
                }
                if (around == null) {
                    readRange(pos, insns.length);
                    break;
                }
                readRange(pos, armEnd);
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
            for (int i = from; i < to; i++) {
                AbstractInsnNode insn = insns[i];
                line = lineAt[i];
                if (frames[i] == null) {
                    continue; // unreachable
                }
                current = i;
                Candidate candidate = candidates.get(i);
                if (candidate != null && candidate.end() <= to && branch(candidate)) {
                    i = candidate.end() - 1;
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
                        add(walk(m.owner, m.name, m.desc, active));
                        sleeps.addAll(SLEEPS.getOrDefault(key(m.owner, m.name, m.desc), List.of()));
                    }
                }
            }
        }

        private void add(List<Step> more) {
            if (!more.isEmpty()) {
                stepAt.add(current);
                flushFork(false);
                steps.addAll(more);
            }
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
                steps.add(new Fork(List.copyOf(branches), joined, site()));
                if (!joined) {
                    unjoined++;
                }
            }
            branches.clear();
        }

        private void call(Command command) {
            if (hasFinally && !seenInFinally.add(line + ":" + command.name)) {
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
            for (int[] range : c.ranges()) {
                List<Step> savedSteps = new ArrayList<>(steps);
                List<List<Step>> savedForks = new ArrayList<>(branches);
                int savedUnjoined = unjoined;
                steps.clear();
                branches.clear();
                readRange(range[0], range[1]);
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
            if (!calls) {
                return false;
            }
            line = lineAt[c.b()];
            int last = line;
            List<Arm> arms = new ArrayList<>();
            for (int k = 0; k < c.ranges().size(); k++) {
                int[] range = c.ranges().get(k);
                Set<Integer> lines = new TreeSet<>();
                for (int i = range[0]; i < range[1]; i++) {
                    if (frames[i] != null && insns[i].getOpcode() >= 0) {
                        last = Math.max(last, lineAt[i]);
                        if (isCall(insns[i])) {
                            lines.add(lineAt[i]);
                        }
                    }
                }
                arms.add(new Arm(c.keys().get(k), k == c.otherwise(), Collections.unmodifiableSet(lines), armSteps.get(k),
                        canSkip(range[0], range[1]), loopsIn(range[0], range[1])));
            }
            Branch b = new Branch(c.name(), lineAt[c.b()], last, List.copyOf(arms), site());
            found.put(c.b(), b);
            flushFork(false);
            steps.add(b.decision());
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
            return new Shape(List.copyOf(sorted), loopsIn(0, insns.length), Collections.unmodifiableSet(returns));
        }

        /** Whether the code can go from {@code from} out of the range without making a call. */
        private boolean canSkip(int from, int to) {
            if (from >= to) {
                return true;
            }
            ArrayDeque<Integer> todo = new ArrayDeque<>(List.of(from));
            Set<Integer> seen = new HashSet<>();
            while (!todo.isEmpty()) {
                int i = todo.pop();
                if (i < from || i >= to || exits(insns[i])) {
                    return true;
                }
                if (!seen.add(i) || stepAt.contains(i) || frames[i] == null) {
                    continue;
                }
                todo.addAll(successors(i));
            }
            return false;
        }

        /** The loops in a range, as their first and last line: a backward jump closes one. */
        private List<int[]> loopsIn(int from, int to) {
            List<int[]> loops = new ArrayList<>();
            for (int i = from; i < to; i++) {
                if (frames[i] != null && insns[i] instanceof JumpInsnNode j && index(j.label) <= i && index(j.label) >= from) {
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

        private void findCandidates() {
            Map<Integer, Integer> backEdge = new HashMap<>();
            for (int i = 0; i < insns.length; i++) {
                if (frames[i] != null && insns[i] instanceof JumpInsnNode j && index(j.label) <= i) {
                    backEdge.merge(index(j.label), i, Math::max);
                }
            }
            // javac evaluates `a && b` as one jump per operand, all to the same place: the last decides
            Map<Long, Candidate> ifs = new LinkedHashMap<>();
            List<Candidate> all = new ArrayList<>();
            for (int b = 0; b < insns.length; b++) {
                if (frames[b] == null) {
                    continue;
                }
                if (insns[b] instanceof JumpInsnNode j && conditional(j.getOpcode())) {
                    Candidate c = ifCandidate(b, index(j.label), backEdge);
                    if (c != null) {
                        long key = ((long) c.ranges().get(1)[0] << 32) | c.end();
                        Candidate before = ifs.put(key, c);
                        if (before != null && !sameCondition(before.b(), b)) {
                            all.add(before);
                        }
                    }
                } else if (insns[b] instanceof TableSwitchInsnNode || insns[b] instanceof LookupSwitchInsnNode) {
                    Candidate c = switchCandidate(b);
                    if (c != null) {
                        all.add(c);
                    }
                }
            }
            all.addAll(ifs.values());
            for (Candidate c : all) {
                if (valid(c)) {
                    candidates.put(c.b(), c);
                }
            }
        }

        /** Two jumps of one condition: the same line, no statement between them. */
        private boolean sameCondition(int first, int second) {
            for (int i = first + 1; i < second; i++) {
                if (insns[i] instanceof LineNumberNode) {
                    return false;
                }
            }
            return lineAt[first] == lineAt[second];
        }

        /**
         * An if: the jump at {@code b} skips the then-arm to {@code t}. A then-arm ending in a jump
         * past the else-arm has one; a jump back to before {@code b} that closes a loop makes the
         * jump the loop's condition, not an if.
         */
        private Candidate ifCandidate(int b, int t, Map<Integer, Integer> backEdge) {
            if (t <= b) {
                return null;
            }
            int before = prevReal(t);
            if (before <= b) {
                return null;
            }
            AbstractInsnNode last = insns[before];
            int end = t;
            if (last instanceof JumpInsnNode lj) {
                int target = index(lj.label);
                if (conditional(lj.getOpcode()) && target > before) {
                    return null; // one operand of an `a || b`
                }
                if (lj.getOpcode() == Opcodes.GOTO && target <= b && backEdge.get(target) == before) {
                    return null; // the condition of a loop
                }
                if (lj.getOpcode() == Opcodes.GOTO && target > t) {
                    boolean leaves = false;
                    for (int i = t; i < target && !leaves; i++) {
                        for (int to : targets(i)) {
                            leaves |= frames[i] != null && to < t;
                        }
                    }
                    if (!leaves) {
                        end = target; // an else arm; otherwise the jump is a break or continue
                    }
                }
            }
            return new Candidate(b, name("if", b), end, List.of(new int[] {b + 1, t}, new int[] {t, end}),
                    List.of("true", "false"), 1);
        }

        /** A switch: an arm per case target, in order, each ending in a break, a return or a throw. */
        private Candidate switchCandidate(int b) {
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
            Map<Integer, String> names = caseNames(b);
            int d = index(dflt);
            TreeMap<Integer, List<String>> byStart = new TreeMap<>();
            for (int k = 0; k < keys.size(); k++) {
                int start = index(labels.get(k));
                if (start <= b) {
                    return null;
                }
                if (start != d) {
                    byStart.computeIfAbsent(start, x -> new ArrayList<>())
                            .add(names.getOrDefault(keys.get(k), String.valueOf(keys.get(k))));
                }
            }
            if (d <= b) {
                return null;
            }
            List<Integer> starts = new ArrayList<>(new TreeSet<>(byStart.keySet()));
            if (!starts.contains(d)) {
                starts.add(d);
                starts.sort(null);
            }
            int lastStart = starts.get(starts.size() - 1);
            Integer end = null;
            for (int k = 1; k < starts.size(); k++) {
                AbstractInsnNode prev = insns[prevReal(starts.get(k))];
                if (prev instanceof JumpInsnNode g && g.getOpcode() == Opcodes.GOTO && index(g.label) >= lastStart) {
                    if (end != null && end != index(g.label)) {
                        return null;
                    }
                    end = index(g.label);
                } else if (!exits(prev) && !(prev instanceof JumpInsnNode g2 && g2.getOpcode() == Opcodes.GOTO)) {
                    return null; // falls through into the next case
                }
            }
            if (end == null) {
                end = reachEnd(lastStart);
            }
            List<int[]> ranges = new ArrayList<>();
            List<String> armKeys = new ArrayList<>();
            int otherwise = -1;
            for (int k = 0; k < starts.size(); k++) {
                int a = starts.get(k);
                if (a >= end) {
                    continue; // no default: the switch's default goes to the code after it
                }
                int z = k + 1 < starts.size() ? Math.min(starts.get(k + 1), end) : end;
                if (a == d) {
                    otherwise = ranges.size();
                    armKeys.add("default");
                } else {
                    armKeys.add(String.join(",", byStart.get(a)));
                }
                ranges.add(new int[] {a, z});
            }
            if (otherwise < 0) {
                otherwise = ranges.size();
                armKeys.add("default");
                ranges.add(new int[] {end, end});
            }
            return new Candidate(b, name("switch", b), end, List.copyOf(ranges), List.copyOf(armKeys), otherwise);
        }

        /** One past the last instruction reachable from {@code from} without going back before it. */
        private int reachEnd(int from) {
            int max = from;
            ArrayDeque<Integer> todo = new ArrayDeque<>(List.of(from));
            Set<Integer> seen = new HashSet<>();
            while (!todo.isEmpty()) {
                int i = todo.pop();
                if (i < from || i >= insns.length || !seen.add(i)) {
                    continue;
                }
                max = Math.max(max, i);
                if (!exits(insns[i])) {
                    todo.addAll(successors(i));
                }
            }
            return max + 1;
        }

        /**
         * Whether a candidate can be followed from the lines calls are made on: each line with a
         * call is in one arm or outside them all, and an arm is entered only from its start.
         */
        private boolean valid(Candidate c) {
            Map<Integer, Integer> regionOfLine = new HashMap<>();
            for (int i = 0; i < insns.length; i++) {
                if (frames[i] != null && isCall(insns[i])) {
                    Integer seen = regionOfLine.putIfAbsent(lineAt[i], region(c, i));
                    if (seen != null && seen != region(c, i)) {
                        return false;
                    }
                }
            }
            for (int s = 0; s < insns.length; s++) {
                if (frames[s] == null) {
                    continue;
                }
                for (int t : targets(s)) {
                    for (int[] r : c.ranges()) {
                        boolean from = s >= r[0] && s < r[1];
                        if (t < r[0] || t >= r[1] || from) {
                            continue;
                        }
                        boolean chain = s < c.b() && lineAt[s] == lineAt[c.b()] && insns[s] instanceof JumpInsnNode;
                        if (t > r[0] || (s != c.b() && !chain)) {
                            return false;
                        }
                    }
                }
            }
            for (TryCatchBlockNode tc : method.tryCatchBlocks) {
                int h = index(tc.handler);
                for (int[] r : c.ranges()) {
                    if (h >= r[0] && h < r[1] && (index(tc.start) < r[0] || index(tc.end) > r[1])) {
                        return false;
                    }
                }
            }
            return true;
        }

        private int region(Candidate c, int i) {
            for (int k = 0; k < c.ranges().size(); k++) {
                if (i >= c.ranges().get(k)[0] && i < c.ranges().get(k)[1]) {
                    return k;
                }
            }
            return -1;
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
                // a string switch: javac numbers the strings, matched by equals, into a local
                for (int i = 0; i < insns.length; i++) {
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

        private int prevReal(int i) {
            int j = i - 1;
            while (j >= 0 && insns[j].getOpcode() < 0) {
                j--;
            }
            return j;
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
