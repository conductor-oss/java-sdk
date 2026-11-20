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
package durability;

import java.util.Arrays;
import java.util.List;

/**
 * A standalone round-robin TCP load balancer, for pointing the kitchensink or any client at the
 * bench's server cluster: {@code java durability.Balancer 8100 8101,8102,8103}.
 */
public final class Balancer {

    private Balancer() {}

    public static void main(String[] args) throws Exception {
        int listen = Integer.parseInt(args[0]);
        List<Integer> upstreams = Arrays.stream(args[1].split(",")).map(String::trim).map(Integer::parseInt).toList();
        if (listen == 8080 || upstreams.contains(8080)) {
            throw new IllegalArgumentException("refusing to use port 8080");
        }
        try (Proxy ignored = new Proxy(listen, "127.0.0.1", upstreams)) {
            System.out.println("balancing 127.0.0.1:" + listen + " across " + upstreams);
            Thread.currentThread().join();
        }
    }
}
