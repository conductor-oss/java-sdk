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
package com.netflix.conductor.client.metrics.prometheus;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractPrometheusMetricsCollectorTest {

    // --- nullToEmpty ---

    @Test
    void nullToEmptyReturnsEmptyForNull() {
        assertEquals("", AbstractPrometheusMetricsCollector.nullToEmpty(null));
    }

    @Test
    void nullToEmptyReturnsOriginalForNonNull() {
        assertEquals("hello", AbstractPrometheusMetricsCollector.nullToEmpty("hello"));
    }

    @Test
    void nullToEmptyReturnsEmptyStringAsIs() {
        assertEquals("", AbstractPrometheusMetricsCollector.nullToEmpty(""));
    }

    // --- exceptionLabel ---

    @Test
    void exceptionLabelReturnsEmptyForNull() {
        assertEquals("", AbstractPrometheusMetricsCollector.exceptionLabel(null));
    }

    @Test
    void exceptionLabelReturnsSimpleClassName() {
        assertEquals("RuntimeException",
                AbstractPrometheusMetricsCollector.exceptionLabel(new RuntimeException()));
    }

    @Test
    void exceptionLabelReturnsDifferentExceptionTypes() {
        assertEquals("IllegalStateException",
                AbstractPrometheusMetricsCollector.exceptionLabel(new IllegalStateException()));
        assertEquals("NullPointerException",
                AbstractPrometheusMetricsCollector.exceptionLabel(new NullPointerException()));
    }

    // --- exceptionLabel: wrapper unwrapping ---

    @Test
    void exceptionLabelUnwrapsExecutionException() {
        Exception inner = new IllegalArgumentException("bad arg");
        ExecutionException wrapper = new ExecutionException(inner);

        assertEquals("IllegalArgumentException",
                AbstractPrometheusMetricsCollector.exceptionLabel(wrapper));
    }

    @Test
    void exceptionLabelUnwrapsCompletionException() {
        Exception inner = new ArithmeticException("divide by zero");
        CompletionException wrapper = new CompletionException(inner);

        assertEquals("ArithmeticException",
                AbstractPrometheusMetricsCollector.exceptionLabel(wrapper));
    }

    @Test
    void exceptionLabelUnwrapsInvocationTargetException() {
        Exception inner = new UnsupportedOperationException("nope");
        InvocationTargetException wrapper = new InvocationTargetException(inner);

        assertEquals("UnsupportedOperationException",
                AbstractPrometheusMetricsCollector.exceptionLabel(wrapper));
    }

    @Test
    void exceptionLabelDoesNotUnwrapNonWrapperExceptions() {
        RuntimeException inner = new RuntimeException("inner");
        IllegalStateException outer = new IllegalStateException("outer", inner);

        assertEquals("IllegalStateException",
                AbstractPrometheusMetricsCollector.exceptionLabel(outer));
    }

    @Test
    void exceptionLabelDoesNotUnwrapWrapperWithNullCause() {
        ExecutionException wrapper = new ExecutionException(null);

        assertEquals("ExecutionException",
                AbstractPrometheusMetricsCollector.exceptionLabel(wrapper));
    }

    @Test
    void exceptionLabelHandlesAnonymousExceptionClass() {
        RuntimeException anon = new RuntimeException("anon") { };
        String label = AbstractPrometheusMetricsCollector.exceptionLabel(anon);
        assertNotNull(label);
        assertFalse(label.isEmpty());
    }

    // --- getRegistry ---

    @Test
    void getRegistryReturnsNonNull() {
        var collector = new LegacyPrometheusMetricsCollector();
        assertNotNull(collector.getRegistry());
    }
}
