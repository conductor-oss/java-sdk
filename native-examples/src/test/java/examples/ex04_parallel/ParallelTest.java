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
package examples.ex04_parallel;

import java.util.List;

import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import examples.ex04_parallel.Parallel.Checked;
import examples.ex04_parallel.Parallel.Order;
import examples.ex04_parallel.Parallel.Quote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelTest {

    @Test
    void picksTheCheapestOfAllQuotes() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory()
                .workflows(Parallel.class)
                .tasks(new Parallel.Tasks());

        Checked checked = env.start(Parallel::check, new Order("A-1", 3, List.of("fedex", "ups"))).result();

        assertTrue(checked.fraudFree());
        assertEquals(new Quote("ups", 390), checked.cheapest());
        assertEquals(2, checked.quotes().size());
    }
}
