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
package examples.ex02_conditionals;

import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import examples.ex02_conditionals.Conditionals.Order;
import examples.ex02_conditionals.Conditionals.Region;
import examples.ex02_conditionals.Conditionals.Shipment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConditionalsTest {

    private final DurableTestEnvironment env = DurableTestEnvironment.inMemory()
            .workflows(Conditionals.class)
            .tasks(new Conditionals.Tasks());

    @Test
    void aLightExpressOrderInTheEu() {
        Shipment shipment = env.start(Conditionals::ship, new Order("A-1", Region.EU, 2, true)).result();
        assertEquals(new Shipment("dhl", "parcel label for A-1", "next day"), shipment);
    }

    @Test
    void aHeavyStandardOrderElsewhere() {
        Shipment shipment = env.start(Conditionals::ship, new Order("B-2", Region.APAC, 45, false)).result();
        assertEquals(new Shipment("local post", "freight label for B-2", "3 to 5 days"), shipment);
    }
}
