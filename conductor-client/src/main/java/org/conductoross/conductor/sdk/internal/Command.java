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
    /** Base for the derived reference name when no explicit reference is given. */
    public final String refBase;
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
    /** For the in-memory engine: duration of a WAIT, in seconds. */
    public long seconds;

    public Command(Kind kind, String name, String refBase) {
        this.kind = kind;
        this.name = name;
        this.refBase = refBase;
    }

    public boolean blocking() {
        return kind != Kind.EVENT && kind != Kind.START_WORKFLOW;
    }

    public String ref(String allocated) {
        return explicitRef != null ? explicitRef : allocated;
    }
}
