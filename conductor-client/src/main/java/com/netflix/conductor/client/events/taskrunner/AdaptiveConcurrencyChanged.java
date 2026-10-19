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
package com.netflix.conductor.client.events.taskrunner;

import lombok.Getter;
import lombok.ToString;

/**
 * Published by the adaptive concurrency controller when a worker's share or the process target changes, and after
 * each queue-depth sample.
 */
@Getter
@ToString
public final class AdaptiveConcurrencyChanged extends TaskRunnerEvent {
    private final String domain;
    private final int targetConcurrency;
    private final int heldSlots;
    private final int queueDepth;
    private final int processTargetConcurrency;
    private final int processHeldSlots;
    private final boolean admissionPaused;
    private final String reason;

    public AdaptiveConcurrencyChanged(String taskType,
                                      String domain,
                                      int targetConcurrency,
                                      int heldSlots,
                                      int queueDepth,
                                      int processTargetConcurrency,
                                      int processHeldSlots,
                                      boolean admissionPaused,
                                      String reason) {
        super(taskType);
        this.domain = domain;
        this.targetConcurrency = targetConcurrency;
        this.heldSlots = heldSlots;
        this.queueDepth = queueDepth;
        this.processTargetConcurrency = processTargetConcurrency;
        this.processHeldSlots = processHeldSlots;
        this.admissionPaused = admissionPaused;
        this.reason = reason;
    }
}
