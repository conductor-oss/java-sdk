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
package examples.ex15_strict_determinism;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.NonDeterminismException;
import org.conductoross.conductor.sdk.Workers;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;

import examples.ex15_strict_determinism.StrictDeterminism.Coupon;
import examples.ex15_strict_determinism.StrictDeterminism.RandomCoupon;
import examples.ex15_strict_determinism.StrictDeterminism.RecordedCoupon;
import examples.ex15_strict_determinism.StrictDeterminism.Tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrictDeterminismTest {

    private static final Map<String, String> STRICT = Map.of("conductor.workflow.all.strictDeterminism", "true");

    /** Workers that are never started against a server: the check runs before anything is sent. */
    private static Workers workers(Conductor conductor, Class<?> workflow) {
        return conductor.workflows(workflow).tasks(new Tasks()).settings(STRICT::get);
    }

    private static ConductorClient nowhere() {
        return ConductorClient.builder().basePath("http://localhost:1/api").build();
    }

    @Test
    void theWorkflowThatAsksTheJvmForRandomValuesAndTheTimeIsRefused() {
        ConductorClient client = nowhere();
        try (Conductor conductor = Conductor.of(client)) {
            NonDeterminismException refused = assertThrows(NonDeterminismException.class,
                    () -> workers(conductor, RandomCoupon.class).start());

            String message = refused.getMessage();
            assertTrue(message.startsWith("Workflow random_coupon cannot be hosted: 3 places in its code are not deterministic."), message);
            assertTrue(message.contains("RandomCoupon.issue:") && message.contains("Math.random()")
                    && message.contains("use Conductor.random()"), message);
            assertTrue(message.contains("RandomCoupon.newCode:") && message.contains("UUID.randomUUID()")
                    && message.contains("use Conductor.once(name, () -> UUID.randomUUID())"), message);
            assertTrue(message.contains("Instant.now()") && message.contains("use Conductor.now()"), message);
            assertTrue(message.contains("To host it anyway: conductor.workflow.random_coupon.strictDeterminism=false"), message);
        } finally {
            client.shutdown();
        }
    }

    @Test
    void theSameWorkflowWithRecordedValuesIsHosted() {
        ConductorClient client = nowhere();
        try (Conductor conductor = Conductor.of(client)) {
            // no server here: the definitions are registered in the background, retried until one answers
            workers(conductor, RecordedCoupon.class).start().close();
        } finally {
            client.shutdown();
        }
    }

    @Test
    void theRecordedWorkflowIssuesACoupon() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory()
                .workflows(RecordedCoupon.class)
                .tasks(new Tasks());

        Coupon coupon = env.start(RecordedCoupon::issue, "ada").result();

        assertTrue(Set.of(10, 20).contains(coupon.percent()), coupon.toString());
        assertEquals(coupon.code(), UUID.fromString(coupon.code()).toString());
        assertNotNull(coupon.issued());
        assertEquals("ada@example.com", coupon.sentTo());
    }
}
