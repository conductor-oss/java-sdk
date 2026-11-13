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
package drawing;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/**
 * The tasks of the generated workflows that check how plain Java control flow is drawn
 * (proposals/native-workflows/spec/check_drawing.py). Each answers with simple arithmetic on its
 * argument, so the checker can work out every result, and with it the path the code takes,
 * without asking the server.
 */
public class GenTasks {

    public enum Color { RED, GREEN, BLUE }

    @WorkerTask("g_a")
    public int a(int x) {
        return Math.floorMod(x * 7 + 3, 5);
    }

    @WorkerTask("g_b")
    public int b(int x) {
        return Math.floorMod(x * 5 + 1, 4);
    }

    @WorkerTask("g_c")
    public int c(int x) {
        return Math.floorMod(x * 3 + 2, 6);
    }

    @WorkerTask("g_s")
    public String s(int x) {
        return switch (Math.floorMod(x * 3 + 1, 4)) {
            case 0 -> "red";
            case 1 -> "green";
            case 2 -> "blue";
            default -> "other";
        };
    }

    @WorkerTask("g_e")
    public Color e(int x) {
        return Color.values()[Math.floorMod(x * 5 + 2, 3)];
    }
}
