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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.EventCall;
import org.conductoross.conductor.sdk.HttpCall;
import org.conductoross.conductor.sdk.TaskCall;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/** A builder kept in a variable sends what it was given each time, not what an earlier call left. */
class BuilderReuseTest {

    @SuppressWarnings("unchecked")
    @Test
    void eachCallOfABuilderSendsItsOwnCommand() {
        List<Command> sent = new ArrayList<>();
        Engine engine = (Engine) Proxy.newProxyInstance(Engine.class.getClassLoader(), new Class<?>[] {Engine.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("call") || method.getName().equals("fire")) {
                        sent.add((Command) args[0]);
                        return method.getReturnType() == void.class ? null : Map.of();
                    }
                    if (method.getReturnType() == void.class) {
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        Engine previous = Engine.CURRENT.get();
        Engine.CURRENT.set(engine);
        try {
            EventCall audit = Conductor.event("audit");
            audit.send(Map.of("first", 1));
            audit.send(Map.of("second", 2));

            HttpCall api = Conductor.http("api").get("http://api/a").header("X-Trace", "1");
            api.asJson();
            api.header("X-Trace", "2").asJson();

            TaskCall lookup = Conductor.task("lookup").input("id", "a");
            lookup.asMap();
            lookup.run();
        } finally {
            Engine.CURRENT.set(previous);
        }

        assertEquals(6, sent.size());
        // an event's payload is its own, not merged with the one before
        assertEquals(Map.of("first", 1), sent.get(0).input);
        assertEquals(Map.of("second", 2), sent.get(1).input);
        // a header changed after a call changes only later calls
        Map<String, Object> first = (Map<String, Object>) sent.get(2).input.get("http_request");
        Map<String, Object> second = (Map<String, Object>) sent.get(3).input.get("http_request");
        assertEquals(Map.of("X-Trace", "1"), first.get("headers"));
        assertEquals(Map.of("X-Trace", "2"), second.get("headers"));
        // each call is a command of its own, typed by its own terminal
        assertNotSame(sent.get(4), sent.get(5));
        assertEquals(Map.class, sent.get(4).resultType);
        assertEquals(Void.class, sent.get(5).resultType);
    }
}
