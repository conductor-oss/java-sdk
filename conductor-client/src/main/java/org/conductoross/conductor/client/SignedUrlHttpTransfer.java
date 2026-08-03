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
package org.conductoross.conductor.client;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import okhttp3.Authenticator;
import okhttp3.CookieJar;
import okhttp3.EventListener;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;

/** Shared HTTP mechanics for signed URLs. No Conductor credentials are permitted on this client. */
final class SignedUrlHttpTransfer {

    private final OkHttpClient client;

    SignedUrlHttpTransfer(OkHttpClient rawClient) {
        this.client = sanitize(rawClient);
    }

    static OkHttpClient sanitize(OkHttpClient rawClient) {
        OkHttpClient.Builder builder = rawClient.newBuilder();
        builder.interceptors().clear();
        builder.networkInterceptors().clear();
        return builder
                .authenticator(Authenticator.NONE)
                .proxyAuthenticator(Authenticator.NONE)
                .cookieJar(CookieJar.NO_COOKIES)
                .eventListener(EventListener.NONE)
                .cache(null)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
    }

    void upload(String signedUrl, Path source, Map<String, String> headers) throws IOException {
        RequestBody body = RequestBody.create(source.toFile(), null);
        executePut(signedUrl, body, headers, "Signed URL upload");
    }

    void uploadRange(
            String signedUrl,
            Path source,
            long offset,
            long length,
            Map<String, String> headers)
            throws IOException {
        executePut(signedUrl, rangeBody(source, offset, length), headers, "Signed URL part upload");
    }

    String uploadRangeReturningHeader(
            String signedUrl,
            Path source,
            long offset,
            long length,
            Map<String, String> headers,
            String responseHeader)
            throws IOException {
        Request request = buildPutRequest(signedUrl, rangeBody(source, offset, length), headers);
        try (Response response = execute(request, "Signed URL part upload")) {
            return response.header(responseHeader);
        }
    }

    void download(String signedUrl, Path destination) throws IOException {
        Request request;
        try {
            request = new Request.Builder().url(requireHttpUrl(signedUrl)).get().build();
        } catch (IllegalArgumentException e) {
            throw new NonRetryableFileTransferException("Invalid signed download URL");
        }

        try (Response response = execute(request, "Signed URL download")) {
            ResponseBody body = response.body();
            if (body == null) {
                throw new NonRetryableFileTransferException(
                        "Signed URL download returned no response body");
            }
            try (var input = body.byteStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (InterruptedIOException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Signed URL download interrupted");
            } catch (IOException e) {
                throw new IOException("Signed URL download failed");
            }
        }
    }

    HttpUrl urlWithQuery(String signedUrl, Map<String, String> queryParameters) throws IOException {
        try {
            HttpUrl.Builder builder = requireHttpUrl(signedUrl).newBuilder();
            queryParameters.forEach(
                    (name, value) -> {
                        builder.removeAllQueryParameters(name);
                        builder.addQueryParameter(name, value);
                    });
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new NonRetryableFileTransferException("Invalid signed upload URL");
        }
    }

    static boolean isHttpUrl(String value) {
        if (value == null) {
            return false;
        }
        try {
            requireHttpUrl(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private void executePut(
            String signedUrl,
            RequestBody body,
            Map<String, String> headers,
            String operation)
            throws IOException {
        Request request = buildPutRequest(signedUrl, body, headers);
        try (Response ignored = execute(request, operation)) {
            // A successful status is the complete response for PUT transfers.
        }
    }

    private Request buildPutRequest(
            String signedUrl, RequestBody body, Map<String, String> headers) throws IOException {
        try {
            Request.Builder builder = new Request.Builder().url(requireHttpUrl(signedUrl)).put(body);
            headers.forEach(builder::header);
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new NonRetryableFileTransferException("Invalid signed upload URL");
        }
    }

    private Response execute(Request request, String operation) throws IOException {
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                int statusCode = response.code();
                response.close();
                throw new FileTransferException(operation, statusCode);
            }
            return response;
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(operation + " interrupted");
        } catch (FileTransferException e) {
            throw e;
        } catch (NonRetryableFileTransferException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException(operation + " failed");
        }
    }

    private static HttpUrl requireHttpUrl(String value) {
        HttpUrl url = HttpUrl.get(value);
        String scheme = url.scheme();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("Signed URL must use HTTP(S)");
        }
        return url;
    }

    private static RequestBody rangeBody(Path source, long offset, long length) {
        return new RequestBody() {
            @Override
            public okhttp3.MediaType contentType() {
                return null;
            }

            @Override
            public long contentLength() {
                return length;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try (FileChannel channel = FileChannel.open(source, StandardOpenOption.READ)) {
                    channel.position(offset);
                    ByteBuffer buffer = ByteBuffer.allocate(8192);
                    long remaining = length;
                    while (remaining > 0) {
                        buffer.clear();
                        buffer.limit((int) Math.min(buffer.capacity(), remaining));
                        int read = channel.read(buffer);
                        if (read < 0) {
                            throw new NonRetryableFileTransferException(
                                    "Source ended before the requested range was read");
                        }
                        if (read == 0) {
                            continue;
                        }
                        sink.write(buffer.array(), 0, read);
                        remaining -= read;
                    }
                }
            }
        };
    }
}
