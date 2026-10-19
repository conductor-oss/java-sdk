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

import java.lang.management.MemoryUsage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JvmResourceSamplerTest {

    private final JvmResourceSampler sampler = new JvmResourceSampler();

    @AfterEach
    void tearDown() {
        sampler.close();
    }

    @Test
    void readsHeapAfterACollection() throws Exception {
        assertTrue(sampler.isHeapSupported());

        System.gc();
        ResourceSampler.HeapSample sample = null;
        for (int i = 0; i < 50; i++) {
            sample = sampler.sampleHeap();
            if (sample != null && sample.collectionAtNanos() != AdaptiveConcurrencyController.NONE) {
                break;
            }
            Thread.sleep(20);
        }

        assertNotNull(sample);
        assertTrue(sample.utilization() > 0 && sample.utilization() < 1, "utilization " + sample.utilization());
        assertNotEquals(AdaptiveConcurrencyController.NONE, sample.collectionAtNanos());
        assertTrue(sample.collectionAtNanos() <= System.nanoTime());
    }

    @Test
    void cpuReadingIgnoresOtherReadersOfProcessCpuLoad() throws Exception {
        // getProcessCpuLoad() measures from the previous caller anywhere in the JVM; a scrape just before the
        // sampler used to turn a busy interval into zero.
        assertTrue(sampler.isCpuSupported());
        var osBean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory
                .getOperatingSystemMXBean();
        assertTrue(Double.isNaN(sampler.sampleCpu()), "the first call has no interval");
        long end = System.nanoTime() + 300_000_000L;
        long x = 0;
        while (System.nanoTime() < end) {
            x += System.nanoTime() & 7;
        }
        osBean.getProcessCpuLoad();
        double cpu = sampler.sampleCpu();
        double oneCore = 1.0 / JvmResourceSampler.availableCpus();
        assertTrue(x > 0);
        assertTrue(cpu >= 0.5 * oneCore && cpu <= 1, "cpu " + cpu + " after a busy interval (one core " + oneCore + ")");
    }

    @Test
    void cpuLoadIsRelativeToTheCpuQuota() {
        assertEquals(1.0, JvmResourceSampler.cpuLoad(500_000_000L, 1_000_000_000L, 0.5), 1e-9);
        assertEquals(0.25, JvmResourceSampler.cpuLoad(500_000_000L, 1_000_000_000L, 2), 1e-9);
        assertEquals(1.0, JvmResourceSampler.cpuLoad(3_000_000_000L, 1_000_000_000L, 2), 1e-9, "clamped");
        assertEquals(0.5, JvmResourceSampler.parseCpuMax("50000 100000\n"), 1e-9);
        assertEquals(-1, JvmResourceSampler.parseCpuMax("max 100000"), 1e-9);
        assertEquals(2.0, JvmResourceSampler.parseCpuQuota("200000", "100000"), 1e-9);
        assertEquals(-1, JvmResourceSampler.parseCpuQuota("-1", "100000"), 1e-9);
    }

    @Test
    void poolUtilizationIsRelativeToThePoolCapacity() {
        long mb = 1024 * 1024;
        // A Serial old generation of 85 MB in a 123 MB heap, full after collection.
        assertEquals(0.99, JvmResourceSampler.poolUtilization(new MemoryUsage(0, 84 * mb + 15 * mb / 100, 85 * mb,
                85 * mb), 123 * mb), 0.01);
        // G1 and ZGC report the heap as the old generation's maximum, or none at all.
        assertEquals(0.5, JvmResourceSampler.poolUtilization(new MemoryUsage(0, 64 * mb, 64 * mb, 128 * mb),
                128 * mb), 1e-9);
        assertEquals(0.25, JvmResourceSampler.poolUtilization(new MemoryUsage(0, 32 * mb, 32 * mb, -1), 128 * mb),
                1e-9);
    }

    @Test
    void cpuReadingIsARatioOrUnavailable() {
        if (!sampler.isCpuSupported()) {
            return;
        }
        double cpu = sampler.sampleCpu();
        assertTrue(Double.isNaN(cpu) || (cpu >= 0 && cpu <= 1), "cpu " + cpu);
    }
}
