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
package durability;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.netflix.conductor.client.exception.ConductorClientException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The client-side history, as Jepsen records it: every operation is an {@code invoke} followed by
 * {@code ok}, {@code fail} (definitely did not happen) or {@code info} (may or may not have
 * happened). Fault injections are recorded in the same file.
 */
final class History implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final OutputStream out;
    private final long origin = System.currentTimeMillis();

    History(Path file) throws IOException {
        this.out = new FileOutputStream(file.toFile(), true);
    }

    void invoke(String process, String f, String key, Object value) {
        write(Map.of("process", process, "type", "invoke", "f", f, "key", key), value, null);
    }

    void ok(String process, String f, String key, Object value) {
        write(Map.of("process", process, "type", "ok", "f", f, "key", key), value, null);
    }

    /** Records a failed operation as {@code fail} or {@code info}, depending on whether it is definite. */
    void failed(String process, String f, String key, Throwable error) {
        write(Map.of("process", process, "type", isDefinite(error) ? "fail" : "info", "f", f, "key", key),
                null, describe(error));
    }

    void nemesis(String type, String fault, String target) {
        write(Map.of("process", "nemesis", "type", type, "f", fault, "key", target), null, null);
    }

    private synchronized void write(Map<String, Object> fields, Object value, String error) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("t", System.currentTimeMillis());
        line.put("rel", System.currentTimeMillis() - origin);
        line.putAll(fields);
        if (value != null) {
            line.put("value", value);
        }
        if (error != null) {
            line.put("error", error);
        }
        try {
            out.write((JSON.writeValueAsString(line) + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Definite: the request never reached the server (connection refused) or the server refused it
     * (4xx). Anything else, a timeout, a reset, a 5xx, may have taken effect.
     */
    static boolean isDefinite(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ConnectException) {
                return true;
            }
            if (t instanceof ConductorClientException cce && cce.getStatusCode() >= 400 && cce.getStatusCode() < 500) {
                return true;
            }
        }
        return false;
    }

    static String describe(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String s = error.getClass().getSimpleName() + ": " + error.getMessage();
        if (root != error) {
            s += " / " + root.getClass().getSimpleName() + ": " + root.getMessage();
        }
        return s.length() > 400 ? s.substring(0, 400) : s;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
