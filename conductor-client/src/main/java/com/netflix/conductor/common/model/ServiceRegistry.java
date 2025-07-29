/*
 * Copyright 2025 Conductor Authors.
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
package com.netflix.conductor.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRegistry {

    private String name;
    private Type type;
    private String serviceURI;
    private List<ServiceMethod> methods = new ArrayList<>();
    private List<RequestParam> requestParams = new ArrayList<>();
    private Config config = new Config();
    private boolean circuitBreakerEnabled = false;

    public enum Type {
        HTTP, gRPC
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Config {
        private OrkesCircuitBreakerConfig circuitBreakerConfig = new OrkesCircuitBreakerConfig();
    }
}
