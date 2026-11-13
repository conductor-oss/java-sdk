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

import java.util.ArrayList;
import java.util.List;

import org.conductoross.conductor.sdk.Conductor;

import com.netflix.conductor.client.http.ConductorClient;

/**
 * Hosts generated workflows for check_drawing.py: registers the classes named on the command line
 * with {@link GenTasks}, prints READY, and runs their continuations and tasks until it is stopped.
 * The checker starts the executions itself, over REST.
 */
public class DrawingHost {

    public static void main(String[] args) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        for (String name : args) {
            classes.add(Class.forName(name));
        }
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host().workflows(classes.toArray(new Class<?>[0])).tasks(new GenTasks()).start();
        System.out.println("READY " + classes.size());
        Thread.currentThread().join();
    }
}
