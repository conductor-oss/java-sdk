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
package org.conductoross.conductor.sdk;

import java.time.Duration;

/** Interval policy for {@link Conductor#repeatUntil}: the first check runs after one interval. */
public final class Backoff {

    private final Duration interval;
    private final double factor;
    private final Duration max;
    private final int attempts;

    private Backoff(Duration interval, double factor, Duration max, int attempts) {
        this.interval = interval;
        this.factor = factor;
        this.max = max;
        this.attempts = attempts;
    }

    public static Backoff every(Duration interval) {
        return new Backoff(interval, 1.0, interval, 10);
    }

    public Backoff factor(double factor) {
        return new Backoff(interval, factor, factor > 1.0 && max.equals(interval) ? Duration.ofDays(1) : max, attempts);
    }

    public Backoff max(Duration max) {
        return new Backoff(interval, factor, max, attempts);
    }

    public Backoff attempts(int attempts) {
        return new Backoff(interval, factor, max, attempts);
    }

    public Duration interval() {
        return interval;
    }

    public double factor() {
        return factor;
    }

    public Duration max() {
        return max;
    }

    public int attempts() {
        return attempts;
    }

    /** The wait before check number {@code iteration}, counting from 1. */
    public Duration delay(int iteration) {
        double seconds = interval.toSeconds() * Math.pow(factor, Math.max(0, iteration - 1));
        return Duration.ofSeconds(Math.max(1, Math.min(Math.round(seconds), max.toSeconds())));
    }
}
