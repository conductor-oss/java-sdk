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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the five simulated task workers as beans. The auto-configured
 * {@code TaskRunnerConfigurer} collects every {@code Worker} bean and starts
 * polling for them. Thread counts are taken from
 * {@code conductor.worker.<taskName>.threadCount} (wired to
 * {@code HARNESS_BATCH_SIZE} in {@code application.yml}).
 *
 * <p>The workflow chains the workers in sequence:
 * quickpulse (1s) &rarr; whisperlink (2s) &rarr; shadowfetch (3s) &rarr;
 * ironforge (4s) &rarr; deepcrawl (5s).
 */
@Configuration
public class HarnessWorkerConfiguration {

    @Bean
    public SimulatedTaskWorker quickpulseWorker(HarnessProperties props) {
        return new SimulatedTaskWorker("java_worker_0", "quickpulse", 1, props.getBatchSize(), props.getPollIntervalMs());
    }

    @Bean
    public SimulatedTaskWorker whisperlinkWorker(HarnessProperties props) {
        return new SimulatedTaskWorker("java_worker_1", "whisperlink", 2, props.getBatchSize(), props.getPollIntervalMs());
    }

    @Bean
    public SimulatedTaskWorker shadowfetchWorker(HarnessProperties props) {
        return new SimulatedTaskWorker("java_worker_2", "shadowfetch", 3, props.getBatchSize(), props.getPollIntervalMs());
    }

    @Bean
    public SimulatedTaskWorker ironforgeWorker(HarnessProperties props) {
        return new SimulatedTaskWorker("java_worker_3", "ironforge", 4, props.getBatchSize(), props.getPollIntervalMs());
    }

    @Bean
    public SimulatedTaskWorker deepcrawlWorker(HarnessProperties props) {
        return new SimulatedTaskWorker("java_worker_4", "deepcrawl", 5, props.getBatchSize(), props.getPollIntervalMs());
    }
}
