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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Which Java loops the calling code is inside. A {@code for} or {@code while} compiles to a backward
 * jump, so the compiled method says which lines each loop covers; the call stack says which line
 * each method is on. A step's loops only name what the execution diagram groups: nothing depends on
 * them when the code is replayed.
 */
public final class CodeLoops {

    private record Loop(int first, int last, String id) {}

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    /** Loops of each method, outermost first, by class, method name and descriptor. */
    private static final Map<String, List<Loop>> LOOPS = new ConcurrentHashMap<>();

    private CodeLoops() {}

    /** The loops around the calling code, outermost first: through lambdas and helper methods. */
    public static List<String> here() {
        List<String> path = new ArrayList<>();
        WALKER.forEach(frame -> {
            String c = frame.getClassName();
            if (c.startsWith("org.conductoross.conductor.sdk.") || c.startsWith("java.") || c.startsWith("jdk.")
                    || TaskStubs.isStubClass(c)) {
                return;
            }
            List<String> inFrame = new ArrayList<>();
            for (Loop loop : loopsOf(frame)) {
                if (loop.first <= frame.getLineNumber() && frame.getLineNumber() <= loop.last) {
                    inFrame.add(loop.id);
                }
            }
            path.addAll(0, inFrame);
        });
        return List.copyOf(path);
    }

    /** The loops of a method around a line, outermost first. */
    public static List<String> around(Class<?> type, String method, String descriptor, int line) {
        List<String> ids = new ArrayList<>();
        for (Loop loop : loopsOf(type, method, descriptor)) {
            if (loop.first <= line && line <= loop.last) {
                ids.add(loop.id);
            }
        }
        return ids;
    }

    /** The loops of a method read from its bytecode around a line, outermost first. */
    public static List<String> around(MethodNode method, String className, int line) {
        List<String> ids = new ArrayList<>();
        for (Loop loop : loops(method, className)) {
            if (loop.first <= line && line <= loop.last) {
                ids.add(loop.id);
            }
        }
        return ids;
    }

    private static List<Loop> loopsOf(StackWalker.StackFrame frame) {
        return loopsOf(frame.getDeclaringClass(), frame.getMethodName(), frame.getDescriptor());
    }

    private static List<Loop> loopsOf(Class<?> type, String method, String descriptor) {
        String key = type.getName() + "#" + method + descriptor;
        return LOOPS.computeIfAbsent(key, k -> read(type, method, descriptor));
    }

    private static List<Loop> read(Class<?> type, String methodName, String descriptor) {
        String resource = type.getName().replace('.', '/') + ".class";
        try (InputStream in = type.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return List.of();
            }
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
            for (MethodNode method : node.methods) {
                if (method.name.equals(methodName) && method.desc.equals(descriptor)) {
                    return loops(method, type.getSimpleName());
                }
            }
        } catch (IOException | RuntimeException e) {
            return List.of(); // loops only draw the diagram; never fail a workflow over one
        }
        return List.of();
    }

    /**
     * The last line of the code right after a loop's conditional jump back, when that code ends by
     * jumping to where the loop's own condition (a jump forward on the loop's first line) exits; 0
     * when it does not.
     */
    private static int armAfter(InsnList code, int head, int at, int firstLine) {
        java.util.Set<Integer> exits = new java.util.HashSet<>();
        int line = 0;
        for (int i = head; i <= at; i++) {
            AbstractInsnNode insn = code.get(i);
            if (insn instanceof LineNumberNode n) {
                line = n.line;
            } else if (insn instanceof JumpInsnNode j && j.getOpcode() != Opcodes.GOTO && line == firstLine
                    && code.indexOf(j.label) > at) {
                exits.add(code.indexOf(j.label));
            }
        }
        if (exits.isEmpty()) {
            return 0;
        }
        // only code the loop itself goes on to: the jump back falling through to it, or a jump from
        // inside the loop to it, and nothing from outside (a switch's next case also follows a loop)
        java.util.Set<Integer> start = new java.util.HashSet<>();
        for (int i = at + 1; i < code.size(); i++) {
            start.add(i);
            if (code.get(i).getOpcode() >= 0) {
                break;
            }
        }
        boolean fromInside = code.get(at).getOpcode() != Opcodes.GOTO;
        boolean fromOutside = false;
        for (int i = 0; i < code.size(); i++) {
            for (int t : jumpTargets(code, code.get(i))) {
                if (start.contains(t)) {
                    if (i >= head && i <= at) {
                        fromInside = true;
                    } else {
                        fromOutside = true;
                    }
                }
            }
        }
        if (!fromInside || fromOutside) {
            return 0;
        }
        // the arm: what runs from just after the jump back, without going back into the loop
        int last = 0;
        boolean out = false;
        java.util.ArrayDeque<Integer> todo = new java.util.ArrayDeque<>(List.of(at + 1));
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        while (!todo.isEmpty()) {
            int i = todo.pop();
            if (i <= at || i >= code.size() || !seen.add(i)) {
                continue;
            }
            if (exits.contains(i)) {
                out = true;
                continue;
            }
            AbstractInsnNode insn = code.get(i);
            if (insn instanceof LineNumberNode n) {
                last = Math.max(last, n.line);
            }
            int op = insn.getOpcode();
            if ((op >= Opcodes.IRETURN && op <= Opcodes.RETURN) || op == Opcodes.ATHROW) {
                continue;
            }
            if (insn instanceof JumpInsnNode j) {
                todo.push(code.indexOf(j.label));
                if (op == Opcodes.GOTO) {
                    continue;
                }
            } else if (insn instanceof org.objectweb.asm.tree.TableSwitchInsnNode t) {
                todo.push(code.indexOf(t.dflt));
                t.labels.forEach(l -> todo.push(code.indexOf(l)));
                continue;
            } else if (insn instanceof org.objectweb.asm.tree.LookupSwitchInsnNode l) {
                todo.push(code.indexOf(l.dflt));
                l.labels.forEach(x -> todo.push(code.indexOf(x)));
                continue;
            }
            todo.push(i + 1);
        }
        return out ? last : 0;
    }

    private static List<Integer> jumpTargets(InsnList code, AbstractInsnNode insn) {
        List<Integer> out = new ArrayList<>();
        if (insn instanceof JumpInsnNode j) {
            out.add(code.indexOf(j.label));
        } else if (insn instanceof org.objectweb.asm.tree.TableSwitchInsnNode t) {
            out.add(code.indexOf(t.dflt));
            t.labels.forEach(l -> out.add(code.indexOf(l)));
        } else if (insn instanceof org.objectweb.asm.tree.LookupSwitchInsnNode l) {
            out.add(code.indexOf(l.dflt));
            l.labels.forEach(x -> out.add(code.indexOf(x)));
        }
        return out;
    }

    /**
     * Whether code at {@code head} is a pattern switch's dispatch: a guard that fails jumps back to
     * it to try the next case, which is no loop in the code.
     */
    private static boolean restartsAPatternSwitch(InsnList code, int head) {
        for (int i = head; i < code.size(); i++) {
            AbstractInsnNode insn = code.get(i);
            if (insn instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode indy) {
                return indy.bsm.getOwner().equals("java/lang/runtime/SwitchBootstraps");
            }
            if (insn instanceof JumpInsnNode || insn instanceof org.objectweb.asm.tree.TableSwitchInsnNode
                    || insn instanceof org.objectweb.asm.tree.LookupSwitchInsnNode
                    || insn instanceof org.objectweb.asm.tree.MethodInsnNode) {
                return false;
            }
        }
        return false;
    }

    /** A backward jump closes a loop: the lines between its target and the jump are the loop. */
    private static List<Loop> loops(MethodNode method, String className) {
        InsnList code = method.instructions;
        Map<Integer, Integer> lastByFirst = new TreeMap<>();
        for (int at = 0; at < code.size(); at++) {
            if (code.get(at) instanceof JumpInsnNode jump && code.indexOf(jump.label) < at
                    && !restartsAPatternSwitch(code, code.indexOf(jump.label))) {
                // the line in effect at the loop's start (a for-each's hasNext has no line entry of
                // its own: its line is the one its iterator() call started)
                int first = Integer.MAX_VALUE;
                int head = code.indexOf(jump.label);
                for (int i = head; i < code.size() && code.get(i).getOpcode() < 0; i++) {
                    if (code.get(i) instanceof LineNumberNode line) {
                        first = line.line; // the loop's own line, given right at its start
                        break;
                    }
                }
                for (int i = head; first == Integer.MAX_VALUE && i >= 0; i--) {
                    if (code.get(i) instanceof LineNumberNode line) {
                        first = line.line;
                    }
                }
                int last = 0;
                for (int i = code.indexOf(jump.label); i <= at; i++) {
                    AbstractInsnNode insn = code.get(i);
                    if (insn instanceof LineNumberNode line) {
                        first = Math.min(first, line.line);
                        last = Math.max(last, line.line);
                    }
                }
                if (last > 0) {
                    // an arm after the last jump back that ends by breaking out (`if (c) { ...; break; }`
                    // or an else that breaks, last in a loop): it jumps to where the loop's condition
                    // exits, so it is in the loop
                    last = Math.max(last, armAfter(code, code.indexOf(jump.label), at, first));
                }
                if (last > 0) {
                    lastByFirst.merge(first, last, Math::max);
                }
            }
        }
        List<Loop> loops = new ArrayList<>();
        lastByFirst.forEach((first, last) -> loops.add(new Loop(first, last, className + "_loop_" + first)));
        loops.sort(Comparator.comparingInt(Loop::first).thenComparing(Comparator.comparingInt(Loop::last).reversed()));
        return loops;
    }
}
