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
package com.netflix.conductor.common.metadata.tasks;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionMetadata {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long serverSendTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long clientReceiveTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long executionStartTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long executionEndTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long clientSendTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long pollNetworkLatency;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long updateNetworkLatency;

    // Additional context as Map for flexibility
    private Map<String, Object> additionalContext = new HashMap<>();
}