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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The worker-side ground truth: an append-only JSON-lines file per worker process recording every
 * task execution and every value the workflow code observed. Each line is written with one
 * {@code write} call, so a SIGKILL loses at most the line being written, never an earlier one.
 *
 * <p>Enabled when {@code LEDGER_DIR} is set; otherwise every call is a no-op, so the same workflow
 * runs in the in-memory test environment.
 */
public final class Ledger {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final OutputStream OUT = open();
    private static final String WORKER = System.getenv().getOrDefault("CONDUCTOR_WORKER_ID", "local");
    private static final long PID = ProcessHandle.current().pid();

    private Ledger() {}

    private static OutputStream open() {
        String dir = System.getenv("LEDGER_DIR");
        if (dir == null || dir.isBlank()) {
            return null;
        }
        try {
            Files.createDirectories(Path.of(dir));
            String worker = System.getenv().getOrDefault("CONDUCTOR_WORKER_ID", "local");
            return new FileOutputStream(
                    Path.of(dir, "ledger-" + worker + "-" + ProcessHandle.current().pid() + ".jsonl").toFile(), true);
        } catch (IOException e) {
            throw new IllegalStateException("cannot open ledger in " + dir, e);
        }
    }

    /** A task attempt began. {@code nonce} pairs it with its end. */
    public static void taskStart(String key, String nonce) {
        write("task-start", key, nonce, null);
    }

    public static void taskEnd(String key, String nonce, Object value) {
        write("task-end", key, nonce, value);
    }

    public static void taskFail(String key, String nonce, String reason) {
        write("task-fail", key, nonce, reason);
    }

    /** A handler applied a message; {@code value} is what it saw afterwards (for forensics). */
    public static void handled(String messageId, Object value) {
        write("handled", messageId, null, value);
    }

    /** A value the workflow code saw while running {@code workflowId}; must be the same on every replay. */
    public static void observe(String workflowId, String name, Object value) {
        write("observe", workflowId + ":" + name, null, value);
    }

    private static void write(String kind, String key, String nonce, Object value) {
        if (OUT == null) {
            return;
        }
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("t", System.currentTimeMillis());
        line.put("pid", PID);
        line.put("worker", WORKER);
        line.put("kind", kind);
        line.put("key", key);
        if (nonce != null) {
            line.put("nonce", nonce);
        }
        if (value != null) {
            line.put("value", value);
        }
        try {
            byte[] bytes = (JSON.writeValueAsString(line) + "\n").getBytes(StandardCharsets.UTF_8);
            synchronized (OUT) {
                OUT.write(bytes);
                OUT.flush();
            }
        } catch (IOException e) {
            throw new IllegalStateException("ledger write failed", e);
        }
    }
}
