/*
 * Copyright 2022 Conductor Authors.
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

import lombok.*;

@EqualsAndHashCode
@ToString
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TerminateWorkflow {

    @Builder.Default
    private String terminationReason = null;

    @Builder.Default
    private String workflowId = null;

    public TerminateWorkflow terminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
        return this;
    }

    public TerminateWorkflow workflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

}
