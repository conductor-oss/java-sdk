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

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.metrics.ApiClientMetrics;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiClientMetricsInterceptorTest {

    private static Request buildRequest(String url) {
        return new Request.Builder().url(url).get().build();
    }

    private static Response buildResponse(Request request, int code) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("OK")
                .build();
    }

    @Test
    void interceptRecordsMetricsOnSuccess() throws IOException {
        ApiClientMetrics metrics = mock(ApiClientMetrics.class);
        var interceptor = new ApiClientMetricsInterceptor(metrics);

        Request request = buildRequest("http://localhost/api/tasks");
        Response response = buildResponse(request, 200);

        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenReturn(response);

        Response result = interceptor.intercept(chain);

        assertEquals(200, result.code());
        verify(metrics).recordRequest(eq("GET"), eq("/api/tasks"), eq(200), any(Duration.class));
    }

    @Test
    void interceptRecordsNegativeStatusOnIOException() throws IOException {
        ApiClientMetrics metrics = mock(ApiClientMetrics.class);
        var interceptor = new ApiClientMetricsInterceptor(metrics);

        Request request = buildRequest("http://localhost/api/tasks");

        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenThrow(new IOException("connection refused"));

        assertThrows(IOException.class, () -> interceptor.intercept(chain));

        verify(metrics).recordRequest(eq("GET"), eq("/api/tasks"), eq(-1), any(Duration.class));
    }

    @Test
    void interceptDoesNotSwallowIOException() throws IOException {
        ApiClientMetrics metrics = mock(ApiClientMetrics.class);
        var interceptor = new ApiClientMetricsInterceptor(metrics);

        Request request = buildRequest("http://localhost/api/tasks");
        IOException expected = new IOException("timeout");

        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenThrow(expected);

        IOException thrown = assertThrows(IOException.class, () -> interceptor.intercept(chain));
        assertSame(expected, thrown);
    }

    @Test
    void interceptWithNullMetricsFallsBackToNoop() throws IOException {
        var interceptor = new ApiClientMetricsInterceptor(null);

        Request request = buildRequest("http://localhost/api/workflows");
        Response response = buildResponse(request, 201);

        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenReturn(response);

        Response result = interceptor.intercept(chain);

        assertNotNull(result);
        assertEquals(201, result.code());
    }

    @Test
    void interceptPassesRequestUnmodified() throws IOException {
        ApiClientMetrics metrics = mock(ApiClientMetrics.class);
        var interceptor = new ApiClientMetricsInterceptor(metrics);

        Request request = buildRequest("http://localhost/api/tasks/123");
        Response response = buildResponse(request, 200);

        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenReturn(response);

        interceptor.intercept(chain);

        verify(chain).proceed(request);
    }

    @Test
    void interceptSwallowsMetricsRecordingFailure() throws IOException {
        ApiClientMetrics metrics = mock(ApiClientMetrics.class);
        doThrow(new RuntimeException("metrics broken"))
                .when(metrics).recordRequest(any(), any(), anyInt(), any());

        var interceptor = new ApiClientMetricsInterceptor(metrics);
        Request request = buildRequest("http://localhost/api/tasks");
        Response response = buildResponse(request, 200);

        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenReturn(response);

        Response result = assertDoesNotThrow(() -> interceptor.intercept(chain));
        assertEquals(200, result.code());
    }

    @Test
    void interceptExtractsEncodedPath() throws IOException {
        ApiClientMetrics metrics = mock(ApiClientMetrics.class);
        var interceptor = new ApiClientMetricsInterceptor(metrics);

        Request request = buildRequest("http://localhost/api/tasks/batch?ids=1,2,3");
        Response response = buildResponse(request, 200);

        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenReturn(response);

        interceptor.intercept(chain);

        verify(metrics).recordRequest(eq("GET"), eq("/api/tasks/batch"), eq(200), any(Duration.class));
    }
}
