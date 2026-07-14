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
package org.conductoross.conductor.ai.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.internal.ToolRegistry;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolContext;
import org.conductoross.conductor.ai.model.ToolDef;

/**
 * Example 51 — Shared State
 *
 * <p>Tools can read and write to {@link ToolContext#getState()}, a map that
 * persists across all tool calls within the same agent execution. This enables
 * tools to accumulate data or pass information between invocations without
 * relying on the LLM to relay state.
 *
 * <p>The server injects the current state as {@code _agent_state} in each
 * tool's input, and the SDK reads back {@code _state_updates} from the output
 * to persist mutations for the next call.
 */
public class Example51SharedState {

    static class ShoppingListTools {

        @Tool(name = "add_item", description = "Add an item to the shared shopping list")
        public Map<String, Object> addItem(String item, ToolContext context) {
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) context.getState()
                .getOrDefault("shopping_list", new ArrayList<>());
            items = new ArrayList<>(items); // ensure mutable copy
            items.add(item);
            context.getState().put("shopping_list", items);
            return Map.of("added", item, "total_items", items.size());
        }

        @Tool(name = "get_list", description = "Get the current shopping list")
        public Map<String, Object> getList(ToolContext context) {
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) context.getState()
                .getOrDefault("shopping_list", new ArrayList<>());
            return Map.of("items", items, "total_items", items.size());
        }

        @Tool(name = "clear_list", description = "Clear the shopping list")
        public Map<String, Object> clearList(ToolContext context) {
            context.getState().put("shopping_list", new ArrayList<>());
            return Map.of("status", "cleared");
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        List<ToolDef> rawTools = ToolRegistry.fromInstance(new ShoppingListTools());
        // getMethods() order is not guaranteed — find by name and match Python order:
        // [add_item, get_list, clear_list]
        ToolDef addItem = rawTools.stream()
            .filter(t -> "add_item".equals(t.getName())).findFirst().get();
        ToolDef getList = rawTools.stream()
            .filter(t -> "get_list".equals(t.getName())).findFirst().get();
        ToolDef clearList = rawTools.stream()
            .filter(t -> "clear_list".equals(t.getName())).findFirst().get();
        List<ToolDef> tools = List.of(addItem, getList, clearList);

        Agent agent = Agent.builder()
            .name("shopping_assistant_51")
            .model(Settings.LLM_MODEL)
            .tools(tools)
            .instructions(
                "You help manage a shopping list. Use add_item to add items, "
                + "get_list to view the list, and clear_list to reset it. "
                + "IMPORTANT: Always add all items first, then call get_list separately "
                + "in a follow-up step to verify the list contents. Never call get_list "
                + "in the same batch as add_item calls.")
            .build();

        AgentResult result = runtime.run(agent,
            "Add milk, eggs, and bread to my shopping list, then show me the list.");
        result.printResult();

        runtime.shutdown();
    }
}
