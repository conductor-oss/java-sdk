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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Builds workflow task definitions, as JSON maps, from commands. */
public final class Entries {

    private static final StackWalker WALKER = StackWalker.getInstance();

    private Entries() {}

    /** The entry of a call the code makes now, at the call site on the stack. */
    public static Map<String, Object> of(Command c, String ref, String lane) {
        return of(c, ref, lane, site());
    }

    public static Map<String, Object> of(Command c, String ref, String lane, String site) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("name", c.name);
        e.put("taskReferenceName", ref);
        e.put("type", type(c.kind));
        Map<String, Object> input = escape(c.input);
        input.putAll(c.rawInput);
        e.put("inputParameters", input);
        e.putAll(c.fields);
        // Optional, so that once its retries are exhausted a failure continues to the lane's next
        // continuation, where the code receives it as TaskFailure, instead of failing the
        // workflow. In native workflows the task keeps its real status (FAILED, TIMED_OUT).
        e.putIfAbsent("optional", true);
        e.put("metadata", metadata(lane, site));
        return e;
    }

    /** An entry's metadata for the code making a call now, at the call site on the stack. */
    public static Map<String, Object> metadata(String lane) {
        return metadata(lane, site());
    }

    public static Map<String, Object> metadata(String lane, String site) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lane", lane);
        if (site != null) {
            m.put("site", site);
        }
        return m;
    }

    /**
     * Marks {@code entry} as the task the code paused on: when it finishes, the server runs
     * continuation {@code n} of {@code lane}, which appends what the code does next after it.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> continuation(Map<String, Object> entry, String lane, int n) {
        Map<String, Object> m = (Map<String, Object>) entry.computeIfAbsent("metadata", k -> new LinkedHashMap<>());
        // the lane's n-th continuation runs when this task ends; the lane is the entry's own
        m.putIfAbsent("lane", lane);
        m.put("continuation", n);
        return entry;
    }

    /**
     * An inline task definition with a retry policy, for server steps such as HTTP and LLM calls
     * that have no registered definition: retried like any Conductor task, with exponential
     * backoff, before the failure reaches the code.
     */
    public static Map<String, Object> retryingDefinition(String name, int retryCount, long delaySeconds) {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("name", name);
        def.put("retryCount", retryCount);
        def.put("retryLogic", "EXPONENTIAL_BACKOFF");
        def.put("retryDelaySeconds", delaySeconds);
        def.put("timeoutSeconds", 0);
        def.put("responseTimeoutSeconds", 0);
        def.put("timeoutPolicy", "RETRY");
        def.put("ownerEmail", "native@conductor.local");
        return def;
    }

    public static String type(Command.Kind kind) {
        return switch (kind) {
            case LLM -> "LLM_CHAT_COMPLETE";
            case MCP_LIST -> "LIST_MCP_TOOLS";
            case MCP_CALL -> "CALL_MCP_TOOL";
            default -> kind.name();
        };
    }

    /** The first stack frame outside the SDK and the JDK: where workflow code made the call. */
    public static String site() {
        return WALKER.walk(
                frames ->
                        frames.filter(f -> {
                                    String c = f.getClassName();
                                    return !c.startsWith("org.conductoross.conductor.sdk.")
                                            && !c.startsWith("java.")
                                            && !c.startsWith("jdk.")
                                            && !TaskStubs.isStubClass(c);
                                })
                                .findFirst()
                                .map(f -> {
                                    String c = f.getClassName();
                                    return c.substring(c.lastIndexOf('.') + 1) + "." + f.getMethodName() + ":" + f.getLineNumber();
                                })
                                .orElse(null));
    }

    /**
     * Makes data safe to place in task input: the server treats {@code ${...}} as an expression, so
     * every literal {@code ${} in a string is written as {@code $${}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T escape(T value) {
        if (value instanceof String s) {
            return (T) s.replace("${", "$${");
        }
        if (value instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), escape(v)));
            return (T) out;
        }
        if (value instanceof List<?> l) {
            List<Object> out = new ArrayList<>();
            l.forEach(v -> out.add(escape(v)));
            return (T) out;
        }
        return value;
    }

    /**
     * Each entry of a list, at any depth: an entry, then the entries in its fork branches, switch
     * cases and loop body.
     */
    @SuppressWarnings("unchecked")
    public static void forEachEntry(List<Map<String, Object>> entries, Consumer<Map<String, Object>> action) {
        if (entries == null) {
            return;
        }
        for (Map<String, Object> e : entries) {
            action.accept(e);
            if (e.get("forkTasks") instanceof List<?> branches) {
                branches.forEach(b -> forEachEntry((List<Map<String, Object>>) b, action));
            }
            if (e.get("decisionCases") instanceof Map<?, ?> cases) {
                cases.values().forEach(c -> forEachEntry((List<Map<String, Object>>) c, action));
            }
            if (e.get("defaultCase") instanceof List<?> d) {
                forEachEntry((List<Map<String, Object>>) d, action);
            }
            if (e.get("loopOver") instanceof List<?> l) {
                forEachEntry((List<Map<String, Object>>) l, action);
            }
        }
    }
}
