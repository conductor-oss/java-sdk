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

import java.io.File;
import java.nio.file.Path;

import org.conductoross.conductor.sdk.testing.DurableReplay;

/**
 * Replays recorded executions of generated workflows against their code, as a worker does when it
 * processes a continuation (check_drawing.py, rule D): each file in the directory named on the
 * command line is {@code <class>-<seed>.json}; prints {@code OK <file>} or {@code BAD <file>: why}.
 */
public class ReplayCheck {

    public static void main(String[] args) throws Exception {
        File[] files = new File(args[0]).listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) {
            return;
        }
        for (File f : files) {
            String cls = f.getName().substring(0, f.getName().lastIndexOf('-'));
            try {
                DurableReplay.assertCompatible(Class.forName("drawing.gen." + cls), Path.of(f.getPath()));
                System.out.println("OK " + f.getName());
            } catch (Throwable e) {
                System.out.println("BAD " + f.getName() + ": " + String.valueOf(e.getMessage()).replace('\n', ' '));
            }
        }
    }
}
