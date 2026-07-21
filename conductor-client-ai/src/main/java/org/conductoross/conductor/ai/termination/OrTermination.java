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
package org.conductoross.conductor.ai.termination;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Terminates when ANY of the given conditions are met.
 */
public class OrTermination extends TerminationCondition {
    private final TerminationCondition left;
    private final TerminationCondition right;

    public OrTermination(TerminationCondition left, TerminationCondition right) {
        this.left = left;
        this.right = right;
    }

    public TerminationCondition getLeft() {
        return left;
    }

    public TerminationCondition getRight() {
        return right;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "or");
        map.put("conditions", Arrays.asList(left.toMap(), right.toMap()));
        return map;
    }
}
