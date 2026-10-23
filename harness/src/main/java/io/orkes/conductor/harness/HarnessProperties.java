/*
 * Copyright 2024 Conductor Authors.
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
package io.orkes.conductor.harness;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Tuning knobs for the harness, bound from the {@code harness.*} namespace (see
 * {@code application.yml}, which maps the {@code HARNESS_*} environment variables
 * onto these properties for backwards compatibility with existing deployments).
 */
@Getter
@Setter
@ConfigurationProperties("harness")
public class HarnessProperties {

    /** Workflows to start per second (governor). */
    private int workflowsPerSec = 2;

    /** Thread count per worker (controls polling concurrency). */
    private int batchSize = 20;

    /** Milliseconds between poll cycles. */
    private int pollIntervalMs = 100;

    /** Control-plane probe rate; 0 disables the probe. */
    private int probeRatePerSec = 0;

    /** When true, run workers only (no metadata registration, governor, or probe). */
    private boolean workersOnly = false;
}
