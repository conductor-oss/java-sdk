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
package primitives;

import java.util.ArrayList;
import java.util.List;

import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.switchOn;

/**
 * Decisions. Two ways to write one, and the execution shows both the same way: a SWITCH task with
 * every arm, the arm taken holding the tasks that ran, and the arms not taken showing what they
 * would have called (previews, never run).
 *
 * <pre>
 *   plain Java: if, else if, else, switch           the decision is read from the compiled code; the
 *   (arrow or classic, over strings, ints, enums,   SWITCH is named after what it decides on (if_rush,
 *   switch expressions)                             switch_size). Only decisions whose arms call the
 *                                                   server are drawn: the rest is just Java.
 *
 *   switchOn(name, value)                           a switch you name yourself, with each case a
 *     .when(key, () -> ...).otherwise(() -> ...)    lambda. It needs no bytecode reading to run: the
 *     .get() for a value, .run() for none           taken case runs as it is; only the previews of
 *                                                   the others are read from the code. A value no
 *                                                   case handles, with no otherwise, fails the run.
 * </pre>
 *
 * Use plain Java by default. Reach for switchOn to name the SWITCH, or when a case is built at
 * runtime.
 */
public class SwitchCase {

    public enum Size { SMALL, MEDIUM, LARGE }

    public record Order(String id, boolean rush, int qty, String region, Size size) {}

    public static class Tasks {
        @WorkerTask("p_ship")
        public String ship(String how) {
            return how;
        }
    }

    private final Tasks tasks;

    public SwitchCase(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "p_switch")
    public List<String> run(Order order) {
        List<String> picked = new ArrayList<>();

        // if / else: SWITCH if_rush with cases "true" and default (the else)
        boolean rush = order.rush();
        if (rush) {
            picked.add(tasks.ship("courier"));
        } else {
            picked.add(tasks.ship("post"));
        }

        // if with no else: the default arm is empty
        boolean bulk = order.qty() > 10;
        if (bulk) {
            picked.add(tasks.ship("pallet"));
        }

        // else if: one SWITCH per if, the second inside the first's else
        int qty = order.qty();
        if (qty == 1) {
            picked.add(tasks.ship("envelope"));
        } else if (qty < 10) {
            picked.add(tasks.ship("box"));
        } else {
            picked.add(tasks.ship("crate"));
        }

        // switch statement over a string, arrow style
        switch (order.region()) {
            case "eu" -> picked.add(tasks.ship("dhl"));
            case "us" -> picked.add(tasks.ship("ups"));
            default -> picked.add(tasks.ship("local"));
        }

        // classic switch over an enum, with break
        Size size = order.size();
        switch (size) {
            case SMALL:
                picked.add(tasks.ship("small-van"));
                break;
            case LARGE:
                picked.add(tasks.ship("truck"));
                break;
            default:
                picked.add(tasks.ship("van"));
        }

        // switch expression: each arm yields what its task returned
        String label = switch (order.region()) {
            case "eu" -> tasks.ship("label-eu");
            default -> tasks.ship("label-intl");
        };
        picked.add(label);

        // switchOn with a value: the SWITCH is named "carrier"
        String carrier = switchOn("carrier", order.region())
                .when("eu", () -> tasks.ship("eu-carrier"))
                .when("us", () -> tasks.ship("us-carrier"))
                .otherwise(() -> tasks.ship("any-carrier"))
                .get();
        picked.add(carrier);

        // switchOn for its effect only, with a case that calls nothing
        switchOn("gift_wrap", order.id().startsWith("gift") ? "yes" : "no")
                .when("yes", () -> picked.add(tasks.ship("wrapped")))
                .when("no", () -> picked.add("plain"))
                .run();

        return picked;
    }
}
