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
package io.orkes.conductor.client.model;

import java.util.List;

import com.netflix.conductor.common.metadata.events.EventHandler;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrkesEventHandler extends EventHandler {
    private List<Tag> tags;

    @Builder(builderMethodName = "orkesBuilder")
    public OrkesEventHandler(String name, String description, String event, String condition, List<Action> actions, boolean active, String evaluatorType, List<Tag> tags) {
        this.setName(name);
        this.setDescription(description);
        this.setEvent(event);
        this.setCondition(condition);
        this.setActions(actions);
        this.setActive(active);
        this.setEvaluatorType(evaluatorType);
        this.tags = tags;
    }
}
