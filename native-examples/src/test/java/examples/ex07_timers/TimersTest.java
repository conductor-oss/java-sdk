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
package examples.ex07_timers;

import java.util.List;

import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import examples.ex07_timers.Timers.Trial;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimersTest {

    @Test
    void sendsEachReminderAfterItsWait() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory()
                .workflows(Timers.class)
                .tasks(new Timers.Tasks());

        List<String> sent = env.start(Timers::remind, new Trial("ana", 2, 3600)).result();

        assertEquals(List.of("reminder 1 sent to ana", "reminder 2 sent to ana"), sent.subList(0, 2));
        assertEquals(3, sent.size());
    }
}
