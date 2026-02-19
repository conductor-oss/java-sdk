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
package com.netflix.conductor.sdk.workflow.utils;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTests {

    @Test
    public void testMapBuilderSimple() {
        Map<String, Object> result = new MapBuilder()
                .add("name", "testWorkflow")
                .add("version", 1)
                .add("timeout", 3600L)
                .build();

        assertEquals(3, result.size());
        assertEquals("testWorkflow", result.get("name"));
        assertEquals(1, result.get("version"));
        assertEquals(3600L, result.get("timeout"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMapBuilderNested() {
        Map<String, Object> result = new MapBuilder()
                .add("name", "parentWorkflow")
                .add("config", new MapBuilder()
                        .add("retries", 3)
                        .add("timeout", "PT30S"))
                .build();

        assertEquals(2, result.size());
        assertEquals("parentWorkflow", result.get("name"));

        Object configObj = result.get("config");
        assertInstanceOf(Map.class, configObj);
        Map<String, Object> config = (Map<String, Object>) configObj;
        assertEquals(3, config.get("retries"));
        assertEquals("PT30S", config.get("timeout"));
    }

    @Test
    public void testInputOutputGetterInput() {
        InputOutputGetter getter = new InputOutputGetter("workflow", InputOutputGetter.Field.input);

        assertEquals("${workflow.input.taskName}", getter.get("taskName"));
        assertEquals("${workflow.input}", getter.getParent());
    }

    @Test
    public void testInputOutputGetterOutput() {
        InputOutputGetter getter = new InputOutputGetter("workflow", InputOutputGetter.Field.output);

        assertEquals("${workflow.output.result}", getter.get("result"));
        assertEquals("${workflow.output}", getter.getParent());
    }

    @Test
    public void testInputOutputGetterMap() {
        InputOutputGetter getter = new InputOutputGetter("workflow", InputOutputGetter.Field.input);

        InputOutputGetter.Map mapHelper = getter.map("foo");
        assertEquals("${workflow.input.foo.bar}", mapHelper.get("bar"));

        // Chained map
        InputOutputGetter.Map nestedMap = mapHelper.map("nested");
        assertEquals("${workflow.input.foo.nested.key}", nestedMap.get("key"));

        // Map toString
        assertEquals("${workflow.input.foo}", mapHelper.toString());

        // Map to list
        InputOutputGetter.List listFromMap = mapHelper.list("items");
        assertEquals("${workflow.input.foo.items[0]}", listFromMap.get(0));
    }

    @Test
    public void testInputOutputGetterList() {
        InputOutputGetter getter = new InputOutputGetter("workflow", InputOutputGetter.Field.input);

        InputOutputGetter.List listHelper = getter.list("items");

        // get(String key, int index)
        assertEquals("${workflow.input.items.name[0]}", listHelper.get("name", 0));
        assertEquals("${workflow.input.items.name[2]}", listHelper.get("name", 2));

        // get(int index)
        assertEquals("${workflow.input.items[0]}", listHelper.get(0));
        assertEquals("${workflow.input.items[5]}", listHelper.get(5));

        // list toString
        assertEquals("${workflow.input.items}", listHelper.toString());

        // Chained list
        InputOutputGetter.List nestedList = listHelper.list("nested");
        assertEquals("${workflow.input.items.nested[1]}", nestedList.get(1));

        // List to map
        InputOutputGetter.Map mapFromList = listHelper.map("entry");
        assertEquals("${workflow.input.items.entry.key}", mapFromList.get("key"));
    }
}
