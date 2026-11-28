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
package examples.ex06_signals;

import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.conductoross.conductor.sdk.testing.TestRun;
import org.junit.jupiter.api.Test;

import examples.ex06_signals.Signals.Cart;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalsTest {

    private final DurableTestEnvironment env = DurableTestEnvironment.inMemory()
            .workflows(Cart.class)
            .tasks(new Signals.Tasks());

    @Test
    void ordersWhatIsInTheCartOnCheckout() {
        TestRun<String> cart = env.start(Cart::shop, "ana");
        assertEquals(1, cart.update(Cart::addItem, "mug"));
        assertEquals(2, cart.update(Cart::addItem, "tea"));
        cart.signal(Cart::checkout);

        assertEquals("ana ordered mug, tea", cart.result());
    }

    @Test
    void aCancelledCartOrdersNothing() {
        TestRun<String> cart = env.start(Cart::shop, "raj");
        cart.signal(Cart::cancel, "changed my mind");

        assertEquals("raj: cancelled (changed my mind)", cart.result());
    }

    @Test
    void aCartNobodyChecksOutIsAbandoned() {
        assertEquals("kim: cart abandoned", env.start(Cart::shop, "kim").result());
    }
}
