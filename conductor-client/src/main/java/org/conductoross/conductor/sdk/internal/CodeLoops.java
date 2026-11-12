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

    /** A backward jump closes a loop: the lines between its target and the jump are the loop. */
    private static List<Loop> loops(MethodNode method, String className) {
        InsnList code = method.instructions;
        Map<Integer, Integer> lastByFirst = new TreeMap<>();
        for (int at = 0; at < code.size(); at++) {
            if (code.get(at) instanceof JumpInsnNode jump && code.indexOf(jump.label) < at) {
                int first = Integer.MAX_VALUE;
                int last = 0;
                for (int i = code.indexOf(jump.label); i <= at; i++) {
                    AbstractInsnNode insn = code.get(i);
                    if (insn instanceof LineNumberNode line) {
                        first = Math.min(first, line.line);
                        last = Math.max(last, line.line);
                    }
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
