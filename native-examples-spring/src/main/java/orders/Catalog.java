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
package orders;

import java.util.Map;

import org.springframework.stereotype.Service;

/** A dependency of the tasks, to show they are ordinary beans. */
@Service
public class Catalog {

    private final Map<String, Double> prices = Map.of("book", 12.5, "pen", 1.25);

    public double price(String item) {
        Double price = prices.get(item);
        if (price == null) {
            throw new IllegalArgumentException("no such item: " + item);
        }
        return price;
    }
}
