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
package examples.ex08_error_handling;

import java.util.List;

import org.conductoross.conductor.sdk.WorkflowFailedException;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import examples.ex08_error_handling.ErrorHandling.Trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorHandlingTest {

    private final DurableTestEnvironment env = DurableTestEnvironment.inMemory()
            .workflows(ErrorHandling.class)
            .tasks(new ErrorHandling.Tasks());

    @Test
    void booksATripTheCardCovers() {
        List<String> log = env.start(ErrorHandling::book, new Trip("ok", 45_000)).result();
        assertEquals(List.of("fares found on attempt 3", "flight booked", "hotel booked", "charged 45000 cents"), log);
    }

    @Test
    void aDeclinedCardUndoesTheBookingsLastFirstAndFailsTheTrip() {
        WorkflowFailedException failure = assertThrows(WorkflowFailedException.class,
                () -> env.start(ErrorHandling::book, new Trip("big", 250_000)).result());
        assertTrue(failure.getMessage().contains("hotel cancelled; flight cancelled"), failure.getMessage());
    }
}
