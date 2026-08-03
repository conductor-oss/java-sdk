/*
 * Copyright 2025 Conductor Authors.
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
package org.conductoross.conductor.ai.plans;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal helpers for serialising plan-value trees.
 *
 * <p>The plan dataclasses ({@link Op#args}, {@link Generate#context},
 * {@link Validation#args}, {@link Action#args}) take {@code Object} so
 * callers can mix primitives, maps, lists, and {@link Ref}. {@code
 * serializeValue} walks that tree and replaces nested {@code Ref}s with
 * their wire form.
 */
final class PlanValues {
    private PlanValues() {}

    @SuppressWarnings("unchecked")
    static Object serializeValue(Object v) {
        if (v instanceof Ref r) return r.toJson();
        if (v instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), serializeValue(e.getValue()));
            }
            return out;
        }
        if (v instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) out.add(serializeValue(item));
            return out;
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> serializeArgs(Map<String, Object> args) {
        return (Map<String, Object>) serializeValue(args);
    }
}
