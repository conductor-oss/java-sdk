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
package org.conductoross.conductor.client.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

final class HttpBodies {

    static final OkHttpClient CLIENT = new OkHttpClient();

    private HttpBodies() {}

    static RequestBody stream(InputStream inputStream, long contentLength) {
        return new RequestBody() {
            @Override public MediaType contentType() { return null; }
            @Override public long contentLength() { return contentLength; }
            @Override public void writeTo(BufferedSink sink) throws IOException {
                try (Source source = Okio.source(inputStream)) {
                    sink.writeAll(source);
                }
            }
        };
    }

    static RequestBody range(Path file, long offset, long length) {
        return new RequestBody() {
            @Override public MediaType contentType() { return null; }
            @Override public long contentLength() { return length; }
            @Override public void writeTo(BufferedSink sink) throws IOException {
                try (InputStream in = Files.newInputStream(file)) {
                    long skipped = 0;
                    while (skipped < offset) {
                        long s = in.skip(offset - skipped);
                        if (s <= 0) break;
                        skipped += s;
                    }
                    byte[] buf = new byte[8192];
                    long remaining = length;
                    while (remaining > 0) {
                        int n = in.read(buf, 0, (int) Math.min(buf.length, remaining));
                        if (n < 0) break;
                        sink.write(buf, 0, n);
                        remaining -= n;
                    }
                }
            }
        };
    }
}
