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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.http.TaskClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdaptiveConcurrencyControllerTest {

    private static final long SECOND = TimeUnit.SECONDS.toNanos(1);

    private final AtomicLong clock = new AtomicLong(TimeUnit.HOURS.toNanos(1));
    private final FakeResourceSampler sampler = new FakeResourceSampler();
    private final TaskClient taskClient = mock(TaskClient.class);
    private AdaptiveConcurrencyController controller;

    @BeforeEach
    void setUp() {
        sampler.cpu = 0.20;
        sampler.heap = new ResourceSampler.HeapSample(0.30, AdaptiveConcurrencyController.NONE);
    }

    @AfterEach
    void tearDown() {
        if (controller != null) {
            List.copyOf(registered).forEach(controller::deregister);
        }
    }

    private final java.util.List<WorkerQueue> registered = new java.util.ArrayList<>();

    private AdaptiveConcurrencyController controller(int initialProcessConcurrency) {
        return controller(new AdaptiveConcurrencyConfig.Builder()
                .withInitialProcessConcurrency(initialProcessConcurrency)
                .build());
    }

    /** Standby polls disabled, so a queue without free share only heartbeats. */
    private AdaptiveConcurrencyController heartbeatOnlyController(int initialProcessConcurrency) {
        return controller(new AdaptiveConcurrencyConfig.Builder()
                .withInitialProcessConcurrency(initialProcessConcurrency)
                .withStandbyPollTimeout(Duration.ZERO)
                .build());
    }

    private AdaptiveConcurrencyController controller(AdaptiveConcurrencyConfig config) {
        controller = new AdaptiveConcurrencyController(config, () -> sampler, clock::get, false);
        return controller;
    }

    private WorkerQueue register(String taskType, String domain) {
        WorkerQueue queue = controller.register(taskClient, taskType, domain, 64, 100, () -> true,
                Executors.defaultThreadFactory(), false, new EventDispatcher<>());
        registered.add(queue);
        return queue;
    }

    private void depths(Map<String, Integer> sizes) {
        when(taskClient.getQueueSizesForTasks(anyList())).thenReturn(sizes);
        controller.sampleDepths();
    }

    private void advance(long nanos) {
        clock.addAndGet(nanos);
    }

    private SlotReservation reserve(WorkerQueue queue) {
        Object outcome = controller.tryReserve(queue, clock.get());
        return outcome instanceof SlotReservation reservation ? reservation : null;
    }

    @Test
    void sharesFollowDepthWithLargestRemainderRounding() {
        controller(32);
        WorkerQueue renderPdf = register("render_pdf", null);
        WorkerQueue sendEmail = register("send_email", null);
        WorkerQueue nightlyReport = register("nightly_report", null);
        controller.activate();
        renderPdf.heldSlots = 12;

        depths(Map.of("render_pdf", 48, "send_email", 6, "nightly_report", 0));

        assertEquals(29, renderPdf.targetConcurrency);
        assertEquals(3, sendEmail.targetConcurrency);
        assertEquals(0, nightlyReport.targetConcurrency);
        renderPdf.heldSlots = 0;
    }

    @Test
    void sharesFollowDepthBeyondTheWorkerMaximum() {
        // Backlogs deeper than the worker maximum must still be weighted by depth, not flattened to the maximum.
        controller(16);
        WorkerQueue deep = register("deep", null);
        WorkerQueue medium = register("medium", null);
        WorkerQueue shallow = register("shallow", null);
        WorkerQueue trickle = register("trickle", null);
        controller.activate();

        depths(Map.of("deep", 600, "medium", 300, "shallow", 150, "trickle", 30));

        assertEquals(9, deep.targetConcurrency);
        assertEquals(4, medium.targetConcurrency);
        assertEquals(2, shallow.targetConcurrency);
        assertEquals(1, trickle.targetConcurrency);
    }

    @Test
    void capReachedByOneQueueIsRedistributedToTheOthers() {
        controller(new AdaptiveConcurrencyConfig.Builder().withInitialProcessConcurrency(20).build());
        WorkerQueue capped = controller.register(taskClient, "capped", null, 4, 100, () -> true,
                Executors.defaultThreadFactory(), false, new EventDispatcher<>());
        registered.add(capped);
        WorkerQueue other = register("other", null);
        WorkerQueue third = register("third", null);
        controller.activate();

        depths(Map.of("capped", 1000, "other", 100, "third", 100));

        assertEquals(4, capped.targetConcurrency, "share never exceeds the worker maximum");
        assertEquals(8, other.targetConcurrency);
        assertEquals(8, third.targetConcurrency);
    }

    @Test
    void unknownDepthGivesEveryQueueAShareOfOne() {
        controller(1);
        WorkerQueue a = register("a", null);
        WorkerQueue b = register("b", null);
        WorkerQueue c = register("c", null);
        controller.activate();

        assertEquals(1, a.targetConcurrency);
        assertEquals(1, b.targetConcurrency);
        assertEquals(1, c.targetConcurrency);
    }

    @Test
    void depthIsSampledWithOneRequestNamingEveryQueue() {
        controller(4);
        register("a", null);
        WorkerQueue b = register("b", "tenant1");
        controller.activate();

        depths(Map.of("a", 3, "tenant1:b", 5));

        verify(taskClient, times(1)).getQueueSizesForTasks(List.of("a", "tenant1:b"));
        assertEquals(5, b.depth);
    }

    @Test
    void failedDepthSampleCountsAsOne() {
        controller(8);
        WorkerQueue a = register("a", null);
        controller.activate();
        depths(Map.of("a", 50));
        assertEquals(8, a.targetConcurrency);

        when(taskClient.getQueueSizesForTasks(anyList())).thenThrow(new RuntimeException("unavailable"));
        controller.sampleDepths();

        assertEquals(1, a.targetConcurrency);
    }

    @Test
    void pausedWorkerHasNoWaitingDemand() {
        controller(8);
        WorkerQueue a = controller.register(taskClient, "a", null, 64, 100, () -> false,
                Executors.defaultThreadFactory(), false, new EventDispatcher<>());
        registered.add(a);
        controller.activate();

        depths(Map.of("a", 50));

        assertEquals(0, a.targetConcurrency);
    }

    @Test
    void reservationIsSizedByShareProcessTargetAndBatch() {
        controller(32);
        WorkerQueue a = register("a", null);
        controller.activate();
        depths(Map.of("a", 100));

        SlotReservation first = reserve(a);
        assertNotNull(first);
        assertEquals(16, first.count());
        SlotReservation second = reserve(a);
        assertNotNull(second);
        assertEquals(16, second.count());
        assertEquals(32, controller.getHeldSlots());
        assertTrue(reserve(a) == null);
    }

    @Test
    void emptyResponseLimitsDemandToOneTaskUntilDelivery() {
        controller(32);
        WorkerQueue a = register("a", null);
        controller.activate();
        depths(Map.of("a", 48));
        assertEquals(32, a.targetConcurrency);

        SlotReservation reservation = reserve(a);
        controller.completePoll(reservation, 0, clock.get());

        assertEquals(1, a.targetConcurrency);
        assertEquals(0, controller.getHeldSlots());
        assertTrue(controller.pollDelayNanos(a) > 0);

        advance(SECOND);
        reservation = reserve(a);
        assertNotNull(reservation);
        assertEquals(1, reservation.count());
        controller.completePoll(reservation, 1, clock.get());
        depths(Map.of("a", 48));
        assertEquals(32, a.targetConcurrency);
    }

    @Test
    void freedProcessSlotGoesToTheQueueFurthestBelowItsShare() {
        controller(10);
        WorkerQueue deep = register("deep", null);
        WorkerQueue shallow = register("shallow", null);
        controller.activate();
        depths(Map.of("deep", 80, "shallow", 20));
        assertEquals(8, deep.targetConcurrency);
        assertEquals(2, shallow.targetConcurrency);

        // The deep queue waits for a process slot with a larger deficit than the shallow queue.
        deep.processWaiters = 1;
        deep.processWaitSequence = 1;
        assertFalse(controller.tryReserve(shallow, clock.get()) instanceof SlotReservation reservation
                && reservation.count() > 0, "no task for the shallow queue while the deep one waits");

        deep.processWaiters = 0;
        Object outcome = controller.tryReserve(shallow, clock.get());
        assertTrue(outcome instanceof SlotReservation reservation && reservation.count() > 0);
    }

    @Test
    void queueWithoutShareSendsOneHeartbeatPerInterval() {
        heartbeatOnlyController(4);
        WorkerQueue idle = register("idle", null);
        WorkerQueue busy = register("busy", null);
        controller.activate();
        depths(Map.of("idle", 0, "busy", 2));
        assertEquals(0, idle.targetConcurrency);

        SlotReservation heartbeat = reserve(idle);
        assertNotNull(heartbeat);
        assertTrue(heartbeat.heartbeat());
        assertEquals(1, heartbeat.count());
        controller.completePoll(heartbeat, 0, clock.get());

        advance(SECOND);
        assertTrue(reserve(idle) == null, "heartbeat is at most once per interval");

        advance(5 * SECOND);
        assertNotNull(reserve(idle), "the next interval allows the next heartbeat");
    }

    @Test
    void idleQueuesTakeTurnsOnTheFreeSlotWithinOneInterval() {
        // An idle process decays to one slot. Every idle queue must still heartbeat each interval, or the server's
        // domain fallback stops routing to the domains that lose, yet no queue is polled without an open slot.
        heartbeatOnlyController(1);
        List<WorkerQueue> idle = List.of(register("a", "partner"), register("b", null), register("c", null),
                register("d", null));
        controller.activate();
        depths(Map.of("partner:a", 0, "b", 0, "c", 0, "d", 0));

        for (WorkerQueue queue : idle) {
            SlotReservation heartbeat = reserve(queue);
            assertNotNull(heartbeat, queue.key.taskType() + " heartbeat");
            assertTrue(heartbeat.heartbeat());
            assertEquals(1, heartbeat.count());
            assertEquals(1, controller.getHeldSlots(), "one open slot, one poll");
            for (WorkerQueue other : idle) {
                if (other != queue && other.outstandingPolls == 0 && other.nextHeartbeatAt <= clock.get()) {
                    assertTrue(reserve(other) == null, "no poll without an open slot");
                }
            }
            controller.completePoll(heartbeat, 0, clock.get());
            advance(TimeUnit.MILLISECONDS.toNanos(20));
        }
        assertEquals(0, controller.getHeldSlots());
        assertTrue(idle.stream().allMatch(q -> q.nextHeartbeatAt > clock.get()), "every queue polled this interval");
    }

    @Test
    void noPollWhileAdmissionIsPaused() {
        controller(4);
        WorkerQueue a = register("a", "partner");
        controller.activate();
        depths(Map.of("partner:a", 0));
        sampler.heap = new ResourceSampler.HeapSample(0.95, clock.get());
        controller.sampleResources();
        assertTrue(controller.isAdmissionPaused());

        assertTrue(reserve(a) == null);
    }

    @Test
    void queueWithHeldShareKeepsOneStandbyPollOpen() {
        controller(4);
        WorkerQueue idle = register("idle", null);
        WorkerQueue busy = register("busy", null);
        controller.activate();
        depths(Map.of("idle", 0, "busy", 2));
        assertEquals(0, idle.targetConcurrency);

        SlotReservation standby = reserve(idle);
        assertNotNull(standby);
        assertTrue(standby.standby());
        assertEquals(1, standby.count());
        assertTrue(reserve(idle) == null, "one standby poll at a time");

        controller.completePoll(standby, 0, clock.get());
        assertEquals(0, idle.consecutiveEmptyPolls, "a standby wait running out is not an empty queue");
        assertEquals(0, controller.pollDelayNanos(idle));
        advance(SECOND);
        SlotReservation next = reserve(idle);
        assertNotNull(next, "the next standby poll follows when the previous one's wait has passed");
        assertTrue(next.standby());
        controller.completePoll(next, 0, clock.get());
        assertEquals(0, controller.getHeldSlots());
    }

    @Test
    void standbyUsesOnlySlotsNoShareClaims() {
        controller(3);
        WorkerQueue busy = register("busy", null);
        WorkerQueue idle = register("idle", null);
        controller.activate();
        depths(Map.of("busy", 3, "idle", 0));
        assertEquals(3, busy.targetConcurrency);
        idle.nextHeartbeatAt = clock.get() + 5 * SECOND;

        assertTrue(reserve(idle) == null, "all three slots belong to the busy queue's share, polled or not");

        depths(Map.of("busy", 2, "idle", 0));
        SlotReservation standby = reserve(idle);
        assertNotNull(standby, "the slot no share claims");
        assertTrue(standby.standby());
        SlotReservation work = reserve(busy);
        assertEquals(2, work.count(), "the share is still whole");
        controller.completePoll(work, 0, clock.get());
        controller.completePoll(standby, 0, clock.get());
    }

    @Test
    void standbyNeedsAFreeProcessSlot() {
        // Unclaimed slots are computed over registered queues, so a deregistered worker's draining tasks could
        // otherwise let a standby poll admit past the process target.
        controller(2);
        WorkerQueue leaving = register("leaving", null);
        WorkerQueue idle = register("idle", null);
        controller.activate();
        depths(Map.of("leaving", 5, "idle", 0));

        SlotReservation work = reserve(leaving);
        assertEquals(2, work.count());
        controller.deregister(leaving);                 // its two accepted tasks are still draining
        registered.remove(leaving);
        idle.nextHeartbeatAt = clock.get() + 5 * SECOND;

        assertTrue(reserve(idle) == null, "every process slot is still held by the departed queue's tasks");
        assertEquals(2, controller.getHeldSlots());
        controller.releaseTaskSlot(leaving);
        controller.releaseTaskSlot(leaving);
    }

    @Test
    void doubleSlotReleaseIsClampedAndReported() {
        controller(4);
        WorkerQueue queue = register("pay", null);
        controller.activate();
        depths(Map.of("pay", 2));
        SlotReservation work = reserve(queue);
        assertEquals(2, work.count());
        controller.completePoll(work, 2, clock.get());  // both delivered: the tasks own the slots

        controller.releaseTaskSlot(queue);
        controller.releaseTaskSlot(queue);
        controller.releaseTaskSlot(queue);              // a release bug fires once too often

        assertEquals(0, controller.getHeldSlots(), "the breach is clamped instead of inflating future grants");
        assertEquals(0, queue.heldSlots);
        SlotReservation next = reserve(queue);
        assertEquals(2, next.count(), "the deficit is not inflated by the breach");
        controller.completePoll(next, 0, clock.get());
    }

    @Test
    void taskSlotAgeRunsFromDeliveryOrLastRelease() {
        controller(4);
        WorkerQueue queue = register("age", null);
        controller.activate();
        depths(Map.of("age", 3));

        SlotReservation work = reserve(queue);
        assertEquals(3, queue.reservedPollSlots, "poll reservations are not task slots");
        advance(10 * SECOND);
        controller.completePoll(work, 2, clock.get());
        assertEquals(0, queue.reservedPollSlots);
        assertEquals(2, queue.heldSlots);
        assertEquals(clock.get(), queue.lastTaskSlotReleaseAt, "the age restarts when an idle queue accepts work");

        advance(10 * SECOND);
        controller.releaseTaskSlot(queue);
        assertEquals(clock.get(), queue.lastTaskSlotReleaseAt, "each task release restarts the age");
        controller.releaseTaskSlot(queue);
    }

    @Test
    void standbyDeliveryKeepsItsSlotAndCountsAsBacklog() {
        controller(4);
        WorkerQueue queue = register("a", null);
        controller.activate();
        depths(Map.of("a", 0));
        assertEquals(0, queue.targetConcurrency);

        SlotReservation standby = reserve(queue);
        controller.completePoll(standby, 1, clock.get());
        assertEquals(1, controller.getHeldSlots(), "the task keeps the slot");
        assertEquals(0, queue.standbyPolls);
        depths(Map.of("a", 0));
        assertEquals(1, queue.targetConcurrency, "the running task counts as backlog");

        SlotReservation next = reserve(queue);
        assertNotNull(next, "the next standby poll goes out at once");
        assertTrue(next.standby());
        controller.completePoll(next, 0, clock.get());
        controller.releaseTaskSlot(queue);
    }

    @Test
    void failedStandbyPollBacksOff() {
        controller(4);
        WorkerQueue queue = register("a", null);
        controller.activate();
        depths(Map.of("a", 0));

        controller.failPoll(reserve(queue), clock.get());
        assertEquals(1, queue.consecutiveEmptyPolls);
        assertTrue(controller.pollDelayNanos(queue) > 0);
        assertEquals(0, controller.getHeldSlots());
    }

    @Test
    void idleProcessKeepsRoomForOneStandbyPerQueue() {
        controller(1);
        register("a", null);
        register("b", "partner");
        register("c", null);
        controller.activate();
        for (int i = 0; i < 12; i++) {
            advance(5 * SECOND);
            controller.sampleResources();
            depths(Map.of("a", 0, "partner:b", 0, "c", 0));
            controller.controlTick();
        }
        assertEquals(3, controller.getTargetConcurrency(), "one standby slot per idle queue, no more");
    }

    @Test
    void noStandbyPollWhenDisabled() {
        heartbeatOnlyController(4);
        WorkerQueue queue = register("a", null);
        controller.activate();
        depths(Map.of("a", 0));

        SlotReservation reservation = reserve(queue);
        assertTrue(reservation.heartbeat());
        controller.completePoll(reservation, 0, clock.get());
        for (int i = 0; i < 24; i++) {
            advance(5 * SECOND);
            controller.sampleResources();
            depths(Map.of("a", 0));
            controller.controlTick();
        }
        assertEquals(1, controller.getTargetConcurrency(), "no room is kept for standby polls");
    }

    @Test
    void dueHeartbeatGetsTheNextOpenSlotBeforeQueuesBelowTheirShare() {
        // Under saturation freed slots went to queues below their share, so an idle domain never polled, went stale
        // and its tasks fell back to another domain. A due heartbeat now takes the next open slot, once per interval.
        controller(2);
        WorkerQueue a = register("a", null);
        WorkerQueue b = register("b", null);
        WorkerQueue starved = register("starved", null);
        WorkerQueue idle = register("idle", "partner");
        controller.activate();
        depths(Map.of("a", 100, "b", 100, "starved", 100, "partner:idle", 0));
        SlotReservation first = reserve(a);
        SlotReservation second = reserve(b);
        controller.completePoll(first, first.count(), clock.get());
        controller.completePoll(second, second.count(), clock.get());
        assertEquals(2, controller.getHeldSlots(), "the process is full");

        assertTrue(reserve(idle) == null, "no open slot, no poll");
        assertTrue(reserve(starved) == null);
        idle.heartbeatWaiters = 1;
        controller.releaseTaskSlot(a);

        assertTrue(reserve(starved) == null, "the open slot goes to the due heartbeat first");
        SlotReservation heartbeat = reserve(idle);
        assertNotNull(heartbeat);
        assertEquals(1, heartbeat.count());
        idle.heartbeatWaiters = 0;
        controller.completePoll(heartbeat, 0, clock.get());
        SlotReservation work = reserve(starved);
        assertNotNull(work, "then to the queue below its share");
        controller.completePoll(work, work.count(), clock.get());
        controller.releaseTaskSlot(b);
        controller.releaseTaskSlot(starved);
    }

    @Test
    void arrivalsAfterAnEmptyPollLiftTheCapAndTheBackoff() {
        // After idle heartbeats came back empty, a burst of 200 waited five seconds on a share of one.
        heartbeatOnlyController(16);
        WorkerQueue a = register("a", null);
        controller.activate();
        depths(Map.of("a", 0));
        for (int i = 0; i < 6; i++) {
            SlotReservation heartbeat = reserve(a);
            assertNotNull(heartbeat);
            controller.completePoll(heartbeat, 0, clock.get());
            advance(6 * SECOND);
        }
        assertTrue(a.consecutiveEmptyPolls > 0);

        depths(Map.of("a", 200));
        assertEquals(16, a.targetConcurrency, "the new arrivals count in full");
        assertEquals(0, controller.pollDelayNanos(a), "and the queue polls without waiting out its backoff");
    }

    @Test
    void undeliverableDepthStaysCappedToNewArrivals() {
        controller(16);
        WorkerQueue limited = register("limited", null);
        controller.activate();
        depths(Map.of("limited", 1000));
        SlotReservation poll = reserve(limited);
        controller.completePoll(poll, 0, clock.get());
        assertEquals(1, limited.targetConcurrency, "the server delivered nothing from a deep queue");

        depths(Map.of("limited", 1003));
        assertEquals(4, limited.targetConcurrency, "only the three new tasks and one more count");
    }

    @Test
    void processTargetGrowsTowardDemandWithHeadroom() {
        controller(4);
        WorkerQueue a = register("a", null);
        controller.activate();
        depths(Map.of("a", 1000));
        controller.sampleResources();

        controller.controlTick();
        assertEquals(5, controller.getTargetConcurrency());
        for (int i = 0; i < 50; i++) {
            advance(5 * SECOND);
            controller.sampleResources();
            depths(Map.of("a", 1000));
            controller.controlTick();
        }
        assertEquals(64, controller.getTargetConcurrency(), "capped at demand, which the worker maximum caps");
        assertEquals(64, a.targetConcurrency);
    }

    @Test
    void idleProcessDecaysToTheMinimumInsteadOfOne() {
        controller(new AdaptiveConcurrencyConfig.Builder()
                .withInitialProcessConcurrency(8)
                .withMinProcessConcurrency(32)
                .build());
        register("a", null);
        controller.activate();
        assertEquals(32, controller.getTargetConcurrency(), "the start is never below the minimum");

        depths(Map.of("a", 1000));
        for (int i = 0; i < 30; i++) {
            advance(5 * SECOND);
            controller.sampleResources();
            depths(Map.of("a", 1000));
            controller.controlTick();
        }
        assertEquals(64, controller.getTargetConcurrency());

        for (int i = 0; i < 60; i++) {
            advance(5 * SECOND);
            controller.sampleResources();
            depths(Map.of("a", 0));
            controller.controlTick();
        }
        assertEquals(32, controller.getTargetConcurrency(), "decay stops at the minimum");
    }

    @Test
    void cpuPressureMayReduceBelowTheMinimum() {
        controller(new AdaptiveConcurrencyConfig.Builder().withMinProcessConcurrency(8).build());
        register("a", null);
        controller.activate();
        depths(Map.of("a", 1000));
        sampler.cpu = 1.0;
        for (int i = 0; i < 4; i++) {
            controller.sampleResources();
            advance(SECOND);
        }
        controller.controlTick();
        assertEquals(6, controller.getTargetConcurrency(), "safety outranks the minimum");

        for (int i = 0; i < 12; i++) {
            advance(5 * SECOND);
            controller.sampleResources();
            depths(Map.of("a", 0));
            controller.controlTick();
        }
        assertTrue(controller.getTargetConcurrency() <= 6, "decay never raises the target back to the minimum");
    }

    @Test
    void minimumMustFitTheCeiling() {
        assertThrows(IllegalArgumentException.class, () -> new AdaptiveConcurrencyConfig.Builder()
                .withMaxProcessConcurrency(16).withMinProcessConcurrency(17).build());
        assertThrows(IllegalArgumentException.class, () -> new AdaptiveConcurrencyConfig.Builder()
                .withMinProcessConcurrency(0).build());
    }

    @Test
    void growthNeedsFreshMetrics() {
        controller(4);
        register("a", null);
        controller.activate();
        depths(Map.of("a", 1000));
        controller.sampleResources();
        advance(6 * SECOND);
        depths(Map.of("a", 1000));

        controller.controlTick();

        assertEquals(4, controller.getTargetConcurrency());
    }

    @Test
    void sustainedCpuPressureReducesOncePerInterval() {
        controller(40);
        register("a", null);
        controller.activate();
        depths(Map.of("a", 1000));
        sampler.cpu = 0.95;
        for (int i = 0; i <= 3; i++) {
            controller.sampleResources();
            advance(SECOND);
        }
        controller.controlTick();
        assertEquals(30, controller.getTargetConcurrency());

        for (int i = 0; i < 7; i++) {
            controller.sampleResources();
            advance(SECOND);
        }
        controller.controlTick();
        assertEquals(30, controller.getTargetConcurrency(), "next reduction waits for the reduction interval");

        for (int i = 0; i < 8; i++) {
            controller.sampleResources();
            advance(SECOND);
        }
        controller.controlTick();
        assertEquals(22, controller.getTargetConcurrency());
    }

    @Test
    void highPostCollectionHeapPausesAdmissionAndResumesBelowThreshold() {
        controller(20);
        WorkerQueue a = register("a", null);
        controller.activate();
        depths(Map.of("a", 1000));

        sampler.heap = new ResourceSampler.HeapSample(0.95, clock.get());
        controller.sampleResources();
        assertTrue(controller.isAdmissionPaused());
        assertTrue(reserve(a) == null);

        advance(SECOND);
        sampler.heap = new ResourceSampler.HeapSample(0.85, clock.get());
        controller.sampleResources();
        assertTrue(controller.isAdmissionPaused(), "a reading between the thresholds stays paused");

        advance(SECOND);
        sampler.heap = new ResourceSampler.HeapSample(0.60, clock.get());
        controller.sampleResources();
        assertFalse(controller.isAdmissionPaused());

        controller.controlTick();
        assertEquals(15, controller.getTargetConcurrency(), "the high collection still costs one reduction");
    }

    @Test
    void heapPressureCapsGrowthAtTheReducedTarget() {
        // Re-growing past the level that caused heap pressure ran tasks out of memory end to end.
        controller(20);
        WorkerQueue a = register("a", null);
        controller.activate();
        depths(Map.of("a", 1000));
        sampler.heap = new ResourceSampler.HeapSample(0.95, clock.get());
        controller.sampleResources();
        advance(SECOND);
        sampler.heap = new ResourceSampler.HeapSample(0.30, clock.get());
        controller.sampleResources();
        controller.controlTick();
        assertEquals(15, controller.getTargetConcurrency());

        for (int i = 0; i < 20; i++) {
            advance(5 * SECOND);
            sampler.heap = new ResourceSampler.HeapSample(0.30, clock.get());
            controller.sampleResources();
            depths(Map.of("a", 1000));
            controller.controlTick();
        }
        assertEquals(15, controller.getTargetConcurrency(), "no growth past the pressure level for two minutes");

        for (int i = 0; i < 6; i++) {
            advance(5 * SECOND);
            sampler.heap = new ResourceSampler.HeapSample(0.30, clock.get());
            controller.sampleResources();
            depths(Map.of("a", 1000));
            controller.controlTick();
        }
        assertTrue(controller.getTargetConcurrency() > 15, "the ceiling relaxes after the hold");
        assertTrue(controller.getTargetConcurrency() <= 21, "one slot per control tick");
        assertEquals(controller.getTargetConcurrency(), a.targetConcurrency);
    }

    @Test
    void heapPressureAfterTheHoldReducesAgain() {
        // End to end the target regrew after the hold, then sat through three more heap pauses unreduced.
        controller(20);
        register("a", null);
        controller.activate();
        depths(Map.of("a", 1000));
        sampler.heap = new ResourceSampler.HeapSample(0.95, clock.get());
        controller.sampleResources();
        controller.controlTick();
        assertEquals(15, controller.getTargetConcurrency());
        for (int i = 0; i < 30; i++) {
            advance(5 * SECOND);
            sampler.heap = new ResourceSampler.HeapSample(0.30, clock.get());
            controller.sampleResources();
            depths(Map.of("a", 1000));
            controller.controlTick();
        }
        int regrown = controller.getTargetConcurrency();
        assertTrue(regrown > 15);

        advance(5 * SECOND);
        sampler.heap = new ResourceSampler.HeapSample(0.95, clock.get());
        controller.sampleResources();
        assertTrue(controller.isAdmissionPaused());
        controller.controlTick();
        assertEquals((int) Math.floor(0.75 * regrown), controller.getTargetConcurrency());
    }

    @Test
    void controlTicksContinueAfterOutOfMemoryErrors() throws InterruptedException {
        // End to end the target never moved again after the heap ran out during a tick.
        var failures = new java.util.concurrent.atomic.AtomicInteger(3);
        controller = new AdaptiveConcurrencyController(new AdaptiveConcurrencyConfig.Builder()
                .withInitialProcessConcurrency(1)
                .withControlInterval(Duration.ofMillis(100))
                .withResourceSampleInterval(Duration.ofMillis(20))
                .withMetricFreshness(Duration.ofSeconds(1))
                .withDepthSampleInterval(Duration.ofMillis(20))
                .withDepthFreshness(Duration.ofSeconds(1))
                .build(), () -> sampler, () -> {
                    if (failures.get() > 0 && failures.decrementAndGet() >= 0) {
                        throw new OutOfMemoryError("injected");
                    }
                    return System.nanoTime();
                }, true);
        when(taskClient.getQueueSizesForTasks(anyList())).thenReturn(Map.of("a", 1000));
        failures.set(0);
        register("a", null);
        controller.activate();
        failures.set(20);

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (controller.getTargetConcurrency() < 4 && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(0, failures.get(), "the injected failures were hit");
        assertTrue(controller.getTargetConcurrency() >= 4, "control ticks continue after the failures");
    }

    @Test
    void growthUsesTheHighestRecentHeapReading() {
        // A low reading right after a high one let the target grow into an OutOfMemoryError end to end.
        controller(5);
        WorkerQueue a = register("a", null);
        controller.activate();
        depths(Map.of("a", 1000));
        sampler.heap = new ResourceSampler.HeapSample(0.10, clock.get());
        controller.sampleResources();
        SlotReservation running = reserve(a);
        controller.completePoll(running, running.count(), clock.get());
        assertEquals(5, controller.getHeldSlots());

        advance(SECOND);
        sampler.heap = new ResourceSampler.HeapSample(0.70, clock.get());
        controller.sampleResources();
        advance(4 * SECOND);
        sampler.heap = new ResourceSampler.HeapSample(0.40, clock.get());
        controller.sampleResources();
        depths(Map.of("a", 1000));
        controller.controlTick();
        assertEquals(5, controller.getTargetConcurrency(), "0.70 of the last 30 seconds leaves no room");

        for (int i = 0; i < 6; i++) {
            advance(5 * SECOND);
            sampler.heap = new ResourceSampler.HeapSample(0.40, clock.get());
            controller.sampleResources();
            depths(Map.of("a", 1000));
            controller.controlTick();
        }
        assertEquals(6, controller.getTargetConcurrency(), "growth resumes once the high reading is 30 seconds old");
        for (int i = 0; i < 5; i++) {
            controller.releaseTaskSlot(a);
        }
    }

    @Test
    void growthStopsWhereProjectedHeapReachesTheGrowthThreshold() {
        controller(10);
        WorkerQueue a = register("a", null);
        controller.activate();
        depths(Map.of("a", 1000));
        sampler.heap = new ResourceSampler.HeapSample(0.10, clock.get());
        controller.sampleResources();
        SlotReservation running = reserve(a);
        assertEquals(10, running.count());
        controller.completePoll(running, 10, clock.get());
        // 0.06 of the heap per slot: at 0.70 one more slot would cross the 0.75 growth threshold.
        advance(SECOND);
        sampler.heap = new ResourceSampler.HeapSample(0.70, clock.get());
        controller.sampleResources();
        controller.controlTick();
        assertEquals(10, controller.getTargetConcurrency());

        // 0.03 per slot at 0.40 once 0.70 is older than 30 seconds: room for eleven, so the ordinary step of two.
        advance(31 * SECOND);
        sampler.heap = new ResourceSampler.HeapSample(0.40, clock.get());
        controller.sampleResources();
        depths(Map.of("a", 1000));
        controller.controlTick();
        assertEquals(12, controller.getTargetConcurrency());
        for (int i = 0; i < 10; i++) {
            controller.releaseTaskSlot(a);
        }
    }

    @Test
    void usedHeapBeforeFirstCollectionNeverPauses() {
        controller(4);
        register("a", null);
        controller.activate();

        sampler.heap = new ResourceSampler.HeapSample(0.97, AdaptiveConcurrencyController.NONE);
        controller.sampleResources();

        assertFalse(controller.isAdmissionPaused());
    }

    @Test
    void idlePausedProcessResumesAtOneAfterDrain() {
        controller(20);
        register("a", null);
        controller.activate();
        long collectionAt = clock.get();
        sampler.heap = new ResourceSampler.HeapSample(0.95, collectionAt);
        controller.sampleResources();
        assertTrue(controller.isAdmissionPaused());

        sampler.heap = new ResourceSampler.HeapSample(0.85, collectionAt + 1);
        advance(29 * SECOND);
        controller.sampleResources();
        assertTrue(controller.isAdmissionPaused());

        advance(SECOND);
        controller.sampleResources();
        assertFalse(controller.isAdmissionPaused());
        assertEquals(1, controller.getTargetConcurrency());

        advance(SECOND);
        controller.sampleResources();
        assertFalse(controller.isAdmissionPaused(), "the stale reading does not re-enter the pause");
    }

    @Test
    void surplusDecaysTowardDemand() {
        controller(32);
        register("a", null);
        controller.activate();
        depths(Map.of("a", 2));

        controller.controlTick();
        advance(30 * SECOND);
        depths(Map.of("a", 2));
        controller.controlTick();
        assertEquals(16, controller.getTargetConcurrency());

        for (int i = 0; i < 5; i++) {
            advance(30 * SECOND);
            depths(Map.of("a", 2));
            controller.controlTick();
        }
        assertEquals(2, controller.getTargetConcurrency(), "never below demand");
    }

    @Test
    void replacementNeedsTheFreedSlotToComeBackToTheSameQueue() {
        controller(10);
        WorkerQueue a = register("a", null);
        WorkerQueue b = register("b", null);
        controller.activate();
        depths(Map.of("a", 50, "b", 50));
        SlotReservation reservation = reserve(a);
        controller.completePoll(reservation, reservation.count(), clock.get());

        assertTrue(controller.isReplacementAllowed(a));

        b.processWaiters = 1;
        b.processWaitSequence = 1;
        assertFalse(controller.isReplacementAllowed(a), "b is further below its share");
        b.processWaiters = 0;

        sampler.heap = new ResourceSampler.HeapSample(0.95, clock.get());
        controller.sampleResources();
        assertFalse(controller.isReplacementAllowed(a), "no replacement while paused");

        for (int i = 0; i < reservation.count(); i++) {
            controller.releaseTaskSlot(a);
        }
    }

    @Test
    void emptyPollBackoffIsCapped() {
        controller(4);
        WorkerQueue a = register("a", null);
        controller.activate();
        for (int i = 0; i < 12; i++) {
            SlotReservation reservation = reserve(a);
            if (reservation == null) {
                advance(10 * SECOND);
                reservation = reserve(a);
            }
            controller.completePoll(reservation, 0, clock.get());
        }
        long delay = controller.pollDelayNanos(a);
        assertTrue(delay <= 6 * SECOND && delay >= 4 * SECOND, "backoff capped at about five seconds: " + delay);
    }

    @Test
    void startupFailsWithoutMetricsUnlessDemandOnly() {
        sampler.cpuSupported = false;
        sampler.heapSupported = false;
        controller(4);
        register("a", null);
        assertThrows(IllegalStateException.class, controller::activate);

        controller(new AdaptiveConcurrencyConfig.Builder().withDemandOnlyEnabled(true).build());
        registered.clear();
        WorkerQueue a = register("a", null);
        controller.activate();
        assertEquals(1, a.targetConcurrency);
    }

    @Test
    void configRejectsInconsistentThresholds() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveConcurrencyConfig.Builder().withHeapResumeThreshold(0.95).build());
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveConcurrencyConfig.Builder().withMaxPollBatch(0).build());
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveConcurrencyConfig.Builder().withDepthFreshness(java.time.Duration.ofMillis(500)).build());
    }

    static class FakeResourceSampler implements ResourceSampler {
        volatile boolean cpuSupported = true;
        volatile boolean heapSupported = true;
        volatile double cpu;
        volatile HeapSample heap;

        @Override
        public boolean isCpuSupported() {
            return cpuSupported;
        }

        @Override
        public boolean isHeapSupported() {
            return heapSupported;
        }

        @Override
        public double sampleCpu() {
            return cpu;
        }

        @Override
        public HeapSample sampleHeap() {
            return heap;
        }

        @Override
        public void close() {}
    }
}
