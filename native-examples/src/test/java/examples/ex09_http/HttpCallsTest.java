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
package examples.ex09_http;

import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import examples.ex09_http.HttpCalls.Request;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** HTTP calls answered by the test: no server and no API. */
class HttpCallsTest {

    @Test
    void readsTheOrderThenCreatesItsShipment() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory().workflows(HttpCalls.class);
        env.http("get_order").respond("""
                {"id": "7", "customer": "kim", "status": "paid", "total": 10, "items": [{"sku": "mug", "qty": 1}]}""");
        env.http("create_shipment").respond("""
                {"shipmentId": "S-9", "carrier": "ups"}""");

        String result = env.start(HttpCalls::ship, new Request("http://api", "7")).result();

        assertEquals("kim's order 7 (1 items) ships as S-9 with ups", result);
    }
}
