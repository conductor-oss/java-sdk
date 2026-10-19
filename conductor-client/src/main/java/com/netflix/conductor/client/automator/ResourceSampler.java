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
package com.netflix.conductor.client.automator;

/**
 * Source of JVM resource observations for {@link AdaptiveConcurrencyController}. Implementations are called from the
 * controller's sampling thread, outside the controller lock.
 */
interface ResourceSampler {

    boolean isCpuSupported();

    boolean isHeapSupported();

    /**
     * @return JVM process CPU utilization as a ratio in [0, 1], or {@code Double.NaN} when the sample is unavailable
     */
    double sampleCpu();

    /**
     * @return the latest heap observation, or {@code null} when the sample failed
     */
    HeapSample sampleHeap();

    /**
     * Releases listeners registered with the JVM.
     */
    void close();

    /**
     * A heap observation.
     *
     * @param utilization        post-collection heap used divided by heap max; current used heap before the first
     *                           collection
     * @param collectionAtNanos  monotonic time of the collection that produced the value, or
     *                           {@link AdaptiveConcurrencyController#NONE} before the first collection
     */
    record HeapSample(double utilization, long collectionAtNanos) {}
}
