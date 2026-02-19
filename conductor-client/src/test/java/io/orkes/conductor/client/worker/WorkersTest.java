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
package io.orkes.conductor.client.worker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkersTest {

    @Test
    void testFluentApi() {
        Workers workers = new Workers();

        Workers afterRootUri = workers.rootUri("http://localhost");
        assertSame(workers, afterRootUri, "rootUri() should return the same Workers instance");

        Workers afterKeyId = workers.keyId("key");
        assertSame(workers, afterKeyId, "keyId() should return the same Workers instance");

        Workers afterSecret = workers.secret("sec");
        assertSame(workers, afterSecret, "secret() should return the same Workers instance");
    }

    @Test
    void testRegisterReturnsFluent() {
        Workers workers = new Workers();
        Workers afterRegister = workers.register("task_name", task -> null);
        assertSame(workers, afterRegister, "register() should return the same Workers instance");
    }

    @Test
    void testStartAllThrowsWithoutRootUri() {
        Workers workers = new Workers();
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                workers::startAll,
                "startAll() should throw IllegalStateException when rootUri is not set");
        assertTrue(ex.getMessage().toLowerCase().contains("root"),
                "Exception message should mention root URI");
    }
}
