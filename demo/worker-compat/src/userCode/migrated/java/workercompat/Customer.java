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
package workercompat;

/**
 * User code migrated for the Jackson 3 SDK: the Jackson 3 annotation is added next to the
 * Jackson 2 one. Each Jackson reads only its own, so this works on SDK 6.x and on Jackson 3.
 */
@com.fasterxml.jackson.databind.annotation.JsonNaming(
        com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
@tools.jackson.databind.annotation.JsonNaming(
        tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Customer {
    public String customerId;
}
