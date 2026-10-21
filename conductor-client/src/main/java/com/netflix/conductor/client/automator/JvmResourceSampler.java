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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.management.GarbageCollectionNotificationInfo;

import com.google.common.annotations.VisibleForTesting;

/**
 * Reads JVM process CPU from the growth of {@code OperatingSystemMXBean.getProcessCpuTime()} between samples, relative
 * to the CPUs the process may use, and post-collection heap from GC notifications, falling back to
 * {@link MemoryPoolMXBean#getCollectionUsage()} when notifications are unavailable.
 * <p>
 * Post-collection heap is the larger of the whole heap's utilization and the fullest long-lived pool's utilization of
 * its own capacity. With a fixed-size old generation (Serial, Parallel) long-lived data can only live there, so an old
 * generation that is full after collection is pressure even while an empty eden keeps the whole-heap ratio near 70%.
 * Long-lived pools are those that support usage thresholds, which excludes eden and survivor spaces under every
 * collector; a pool's reading is taken only from collections that collect it, since after a young collection the old
 * generation still holds its uncollected garbage.
 * <p>
 * {@code getProcessCpuLoad()} is not used: it measures the interval since the previous call by any caller in the JVM,
 * so a metrics scrape reading it just before this sampler leaves a window of milliseconds and a reading of zero on a
 * saturated core.
 */
class JvmResourceSampler implements ResourceSampler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JvmResourceSampler.class);

    private static final Path CGROUP_V2_CPU_MAX = Path.of("/sys/fs/cgroup/cpu.max");
    private static final Path CGROUP_V1_CPU_QUOTA = Path.of("/sys/fs/cgroup/cpu/cpu.cfs_quota_us");
    private static final Path CGROUP_V1_CPU_PERIOD = Path.of("/sys/fs/cgroup/cpu/cpu.cfs_period_us");

    private final com.sun.management.OperatingSystemMXBean operatingSystemBean;
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private final List<GarbageCollectorMXBean> collectorBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final List<MemoryPoolMXBean> heapPools;
    private final Set<String> heapPoolNames;
    private final Set<String> longLivedPoolNames;
    /** Collector name to the long-lived pools it collects. */
    private final Map<String, Set<String>> collectedLongLivedPools = new HashMap<>();
    /** Long-lived pool utilization after the latest collection of that pool; written by the GC notification thread. */
    private final Map<String, Double> longLivedUtilization = new ConcurrentHashMap<>();
    private final List<NotificationEmitter> emitters = new ArrayList<>();
    private final NotificationListener gcListener = this::onCollection;
    private final boolean heapSupported;

    // Written by the GC notification thread, read by the sampling thread; null until the first notification.
    private volatile CollectionResult lastNotified;

    // CPU state, sampling thread only.
    private long lastCpuTime = -1;
    private long lastCpuSampledAt;

    // Fallback path state, sampling thread only.
    private long lastCollectionCount = -1;
    private long lastCollectionAt = AdaptiveConcurrencyController.NONE;

    /** Collections tolerated without a notification before the notification pipeline counts as stalled. */
    private static final int NOTIFICATION_STALL_COLLECTIONS = 4;
    // Stall detection, sampling thread only: re-reading one notification forever while collections continue would
    // freeze the heap estimate at a stale value that still looks fresh.
    private CollectionResult lastSeenNotification;
    private long collectionCountAtNotification;
    private boolean notificationStallLogged;

    JvmResourceSampler() {
        var osBean = ManagementFactory.getOperatingSystemMXBean();
        this.operatingSystemBean = osBean instanceof com.sun.management.OperatingSystemMXBean sunBean ? sunBean : null;
        this.heapPools = ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == MemoryType.HEAP)
                .collect(Collectors.toList());
        this.heapPoolNames = heapPools.stream().map(MemoryPoolMXBean::getName).collect(Collectors.toSet());
        this.longLivedPoolNames = heapPools.stream()
                .filter(MemoryPoolMXBean::isUsageThresholdSupported)
                .map(MemoryPoolMXBean::getName)
                .collect(Collectors.toSet());
        for (GarbageCollectorMXBean collector : collectorBeans) {
            Set<String> collected = new HashSet<>(List.of(collector.getMemoryPoolNames()));
            collected.retainAll(longLivedPoolNames);
            collectedLongLivedPools.put(collector.getName(), collected);
        }

        for (GarbageCollectorMXBean collector : collectorBeans) {
            if (collector instanceof NotificationEmitter emitter) {
                try {
                    emitter.addNotificationListener(gcListener, null, null);
                    emitters.add(emitter);
                } catch (RuntimeException e) {
                    LOGGER.warn("Unable to register GC notification listener on {}", collector.getName(), e);
                }
            }
        }
        boolean collectionUsageAvailable = heapPools.stream().anyMatch(pool -> pool.getCollectionUsage() != null);
        this.heapSupported = heapMax() > 0 && (!emitters.isEmpty() || collectionUsageAvailable);
    }

    @Override
    public boolean isCpuSupported() {
        if (operatingSystemBean == null) {
            return false;
        }
        try {
            return operatingSystemBean.getProcessCpuTime() >= 0;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    @Override
    public boolean isHeapSupported() {
        return heapSupported;
    }

    /**
     * @return CPU used since the previous call as a fraction of the CPUs available to the process; NaN on the first
     *         call and when unavailable
     */
    @Override
    public double sampleCpu() {
        try {
            long cpuTime = operatingSystemBean.getProcessCpuTime();
            long sampledAt = System.nanoTime();
            long previousCpuTime = lastCpuTime;
            long previousSampledAt = lastCpuSampledAt;
            lastCpuTime = cpuTime;
            lastCpuSampledAt = sampledAt;
            if (cpuTime < 0 || previousCpuTime < 0 || sampledAt <= previousSampledAt) {
                return Double.NaN;
            }
            return cpuLoad(cpuTime - previousCpuTime, sampledAt - previousSampledAt, availableCpus());
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    static double cpuLoad(long cpuNanos, long elapsedNanos, double cpus) {
        double load = (double) cpuNanos / ((double) elapsedNanos * cpus);
        return Double.isFinite(load) ? Math.min(1, Math.max(0, load)) : Double.NaN;
    }

    /**
     * The CPUs the process may use: the container CPU quota when one is set, which may be fractional, otherwise the
     * processors the JVM reports. Read on every sample, so a resized container takes effect.
     */
    static double availableCpus() {
        int processors = Runtime.getRuntime().availableProcessors();
        double quota = cgroupCpuLimit();
        return quota > 0 ? Math.min(processors, quota) : processors;
    }

    private static double cgroupCpuLimit() {
        try {
            if (Files.isReadable(CGROUP_V2_CPU_MAX)) {
                return parseCpuMax(Files.readString(CGROUP_V2_CPU_MAX));
            }
            if (Files.isReadable(CGROUP_V1_CPU_QUOTA) && Files.isReadable(CGROUP_V1_CPU_PERIOD)) {
                return parseCpuQuota(Files.readString(CGROUP_V1_CPU_QUOTA), Files.readString(CGROUP_V1_CPU_PERIOD));
            }
        } catch (Exception e) {
            LOGGER.debug("Unable to read the cgroup CPU limit", e);
        }
        return -1;
    }

    /**
     * @param cpuMax cgroup v2 {@code cpu.max}: {@code "<quota> <period>"} or {@code "max <period>"}
     * @return the limit in CPUs, or -1 when unlimited or unreadable
     */
    static double parseCpuMax(String cpuMax) {
        String[] parts = cpuMax.trim().split("\\s+");
        if (parts.length != 2 || "max".equals(parts[0])) {
            return -1;
        }
        return parseCpuQuota(parts[0], parts[1]);
    }

    /**
     * @return {@code quota / period} in CPUs, or -1 when the quota is unlimited ({@code -1}) or unreadable
     */
    static double parseCpuQuota(String quota, String period) {
        try {
            long quotaMicros = Long.parseLong(quota.trim());
            long periodMicros = Long.parseLong(period.trim());
            return quotaMicros > 0 && periodMicros > 0 ? (double) quotaMicros / periodMicros : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public HeapSample sampleHeap() {
        try {
            long max = heapMax();
            if (max <= 0) {
                return null;
            }
            long collectionCount = totalCollectionCount();
            if (collectionCount <= 0) {
                // No collection yet: current used heap is an upper bound on live data.
                return new HeapSample((double) memoryBean.getHeapMemoryUsage().getUsed() / (double) max,
                        AdaptiveConcurrencyController.NONE);
            }
            CollectionResult notified = lastNotified;
            if (notified != null) {
                if (notified != lastSeenNotification) {
                    lastSeenNotification = notified;
                    collectionCountAtNotification = collectionCount;
                    // Keep the fallback primed so a later stall can detect the next collection and its time.
                    lastCollectionCount = collectionCount;
                    lastCollectionAt = notified.collectionAtNanos();
                    if (notificationStallLogged) {
                        notificationStallLogged = false;
                        LOGGER.info("GC notifications resumed");
                    }
                }
                if (collectionCount - collectionCountAtNotification <= NOTIFICATION_STALL_COLLECTIONS) {
                    return new HeapSample(notified.utilization(), notified.collectionAtNanos());
                }
                if (!notificationStallLogged) {
                    notificationStallLogged = true;
                    LOGGER.warn("{} collections happened without a GC notification; reading collection usage "
                                    + "until notifications resume",
                            collectionCount - collectionCountAtNotification);
                }
            }
            return sampleCollectionUsage(max, collectionCount);
        } catch (RuntimeException e) {
            LOGGER.debug("Heap sample failed", e);
            return null;
        }
    }

    @Override
    public void close() {
        for (NotificationEmitter emitter : emitters) {
            try {
                emitter.removeNotificationListener(gcListener);
            } catch (Exception e) {
                LOGGER.debug("Unable to remove GC notification listener", e);
            }
        }
        emitters.clear();
    }

    private HeapSample sampleCollectionUsage(long max, long collectionCount) {
        // Collection usage is per pool and only updated by collections of that pool, which is the rule we want.
        long used = 0;
        double longLived = 0;
        for (MemoryPoolMXBean pool : heapPools) {
            MemoryUsage usage = pool.getCollectionUsage();
            if (usage != null) {
                used += usage.getUsed();
                if (longLivedPoolNames.contains(pool.getName())) {
                    longLived = Math.max(longLived, poolUtilization(usage, max));
                }
            }
        }
        if (collectionCount != lastCollectionCount) {
            // A collection happened since the previous tick; the tick that observed it is its time.
            // Collections that completed before sampling started carry no time.
            lastCollectionAt = lastCollectionCount < 0 ? AdaptiveConcurrencyController.NONE : System.nanoTime();
            lastCollectionCount = collectionCount;
        }
        return new HeapSample(Math.max((double) used / (double) max, longLived), lastCollectionAt);
    }

    private void onCollection(Notification notification, Object handback) {
        if (!GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
            return;
        }
        try {
            var info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
            long max = heapMax();
            if (max <= 0) {
                return;
            }
            Map<String, MemoryUsage> after = info.getGcInfo().getMemoryUsageAfterGc();
            long used = 0;
            for (Map.Entry<String, MemoryUsage> entry : after.entrySet()) {
                if (heapPoolNames.contains(entry.getKey())) {
                    used += entry.getValue().getUsed();
                }
            }
            for (String pool : collectedLongLivedPools.getOrDefault(info.getGcName(), Set.of())) {
                MemoryUsage usage = after.get(pool);
                if (usage != null) {
                    longLivedUtilization.put(pool, poolUtilization(usage, max));
                }
            }
            double utilization = (double) used / (double) max;
            for (double pool : longLivedUtilization.values()) {
                utilization = Math.max(utilization, pool);
            }
            // GC end time is JVM uptime in milliseconds; convert it to the monotonic clock.
            long agoMillis = Math.max(0, runtimeBean.getUptime() - info.getGcInfo().getEndTime());
            lastNotified = new CollectionResult(utilization, System.nanoTime() - agoMillis * 1_000_000L);
        } catch (RuntimeException e) {
            LOGGER.debug("Unable to read GC notification", e);
        }
    }

    private record CollectionResult(double utilization, long collectionAtNanos) {}

    /** Simulates a notification that no longer updates, for stall-detection tests. */
    @VisibleForTesting
    void injectNotificationForTest(double utilization) {
        lastNotified = new CollectionResult(utilization, System.nanoTime());
    }

    /** A pool's used share of its own capacity, or of the heap when the pool has no fixed maximum. */
    static double poolUtilization(MemoryUsage usage, long heapMax) {
        long capacity = usage.getMax() > 0 ? Math.min(usage.getMax(), heapMax) : heapMax;
        return Math.min(1, (double) usage.getUsed() / (double) capacity);
    }

    private long heapMax() {
        return memoryBean.getHeapMemoryUsage().getMax();
    }

    private long totalCollectionCount() {
        long total = 0;
        for (GarbageCollectorMXBean collector : collectorBeans) {
            long count = collector.getCollectionCount();
            if (count > 0) {
                total += count;
            }
        }
        return total;
    }
}
