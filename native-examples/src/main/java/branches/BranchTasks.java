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
package branches;

import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import branches.Branches.Color;

/** The tasks of {@link Branches}: each says what it was called with, so a result shows the path taken. */
public class BranchTasks {

    @WorkerTask("br_a")
    public String a(@InputParam("x") String x) {
        return "a(" + x + ")";
    }

    @WorkerTask("br_b")
    public String b(@InputParam("x") String x) {
        return "b(" + x + ")";
    }

    @WorkerTask("br_c")
    public String c(@InputParam("x") String x) {
        return "c(" + x + ")";
    }

    @WorkerTask("br_d")
    public String d(@InputParam("x") String x) {
        return "d(" + x + ")";
    }

    @WorkerTask("br_e")
    public String e(@InputParam("x") String x) {
        return "e(" + x + ")";
    }

    @WorkerTask("br_flag")
    public boolean flag(@InputParam("x") String x) {
        return x.startsWith("y");
    }

    /** A number when the input is one, else the text. */
    @WorkerTask("br_value")
    public Object value(@InputParam("x") String x) {
        try {
            return Integer.parseInt(x);
        } catch (NumberFormatException e) {
            return x;
        }
    }

    @WorkerTask("br_color")
    public Color color(@InputParam("x") String x) {
        return switch (x) {
            case "1" -> Color.RED;
            case "2" -> Color.GREEN;
            default -> Color.BLUE;
        };
    }

    @WorkerTask("br_pick")
    public String pick(@InputParam("x") String x) {
        return switch (x) {
            case "1" -> "left";
            case "2" -> "right";
            default -> "middle";
        };
    }

    @WorkerTask("br_reject")
    public String reject(@InputParam("order") String order) {
        return "rejected " + order;
    }

    @WorkerTask("br_charge")
    public String charge(@InputParam("order") String order) {
        return "charged " + order;
    }

    @WorkerTask("br_ship")
    public String ship(@InputParam("order") String order) {
        return "shipped " + order;
    }
}
