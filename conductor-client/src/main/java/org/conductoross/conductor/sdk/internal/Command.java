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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One call from workflow code to the server, described independently of how it is executed. The
 * replay engine turns it into a workflow task definition; the in-memory test engine answers it
 * directly.
 */
public final class Command {

    public enum Kind {
        SIMPLE,
        SUB_WORKFLOW,
        START_WORKFLOW,
        WAIT,
        HUMAN,
        HTTP,
        EVENT,
        LLM,
        MCP_LIST,
        MCP_CALL,
        AGENT
    }

    public final Kind kind;
    /** Task name: the worker task name, sub-workflow name, or a server step's name. */
    public final String name;
    public String explicitRef;
    public final Map<String, Object> input = new LinkedHashMap<>();
    /** Type-specific definition fields: subWorkflowParam, sink, taskDefinition and so on. */
    public final Map<String, Object> fields = new LinkedHashMap<>();
    /** Type the result decodes into. */
    public Type resultType = Object.class;
    /** A TIMED_OUT task means "no result" rather than a failure (human tasks). */
    public boolean timeoutIsEmpty;
    /** A completed task with empty output means "no result" (external waits). */
    public boolean emptyIsEmpty;
    /** Whether the result is wrapped in Optional. */
    public boolean optional;
    /** HTTP responses are read from response.body. */
    public boolean httpBody;
    /** The result is this key of the task output rather than the whole output. */
    public String outputKey;
    /** Input values that are server expressions, placed without escaping. */
    public final Map<String, Object> rawInput = new LinkedHashMap<>();

    /** For the in-memory engine: the worker method and arguments, when known. */
    public Method method;
    public Object[] args;
    /** For the in-memory engine: the child workflow method. */
    public Method childMethod;
    public Object childInput;

    public Command(Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    /**
     * A copy to send: a builder keeps its command as a template and sends a copy per call, so a
     * builder used twice sends what it was given each time, not what an earlier call left behind.
     * Maps one level down (an HTTP request, headers) are copied too.
     */
    public Command copy() {
        Command c = new Command(kind, name);
        c.explicitRef = explicitRef;
        input.forEach((k, v) -> c.input.put(k, copied(v)));
        fields.forEach((k, v) -> c.fields.put(k, copied(v)));
        rawInput.forEach((k, v) -> c.rawInput.put(k, copied(v)));
        c.resultType = resultType;
        c.timeoutIsEmpty = timeoutIsEmpty;
        c.emptyIsEmpty = emptyIsEmpty;
        c.optional = optional;
        c.httpBody = httpBody;
        c.outputKey = outputKey;
        c.method = method;
        c.args = args;
        c.childMethod = childMethod;
        c.childInput = childInput;
        return c;
    }

    private static Object copied(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(k, copied(v)));
            return copy;
        }
        return value;
    }

    public boolean blocking() {
        return kind != Kind.EVENT && kind != Kind.START_WORKFLOW;
    }

    /** What the reference name is allocated from: the one given in the code, else the name. */
    public String ref() {
        return explicitRef != null ? explicitRef : name;
    }
}
