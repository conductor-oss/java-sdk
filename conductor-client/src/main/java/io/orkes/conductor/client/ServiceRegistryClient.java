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
package io.orkes.conductor.client;

import java.util.List;

import com.netflix.conductor.common.model.CircuitBreakerTransitionResponse;
import com.netflix.conductor.common.model.ProtoRegistryEntry;
import com.netflix.conductor.common.model.ServiceMethod;
import com.netflix.conductor.common.model.ServiceRegistry;

/**
 * Client for managing service registry and gRPC/Protocol Buffer integrations.
 * <p>
 * The service registry allows you to:
 * <ul>
 * <li>Register external gRPC services for use in workflows</li>
 * <li>Manage service methods and their configurations</li>
 * <li>Control circuit breakers for service resilience</li>
 * <li>Upload and manage Protocol Buffer (.proto) definition files</li>
 * <li>Automatically discover service methods from proto files</li>
 * </ul>
 */
public interface ServiceRegistryClient {

    // Service CRUD operations

    /**
     * Retrieves all registered services.
     *
     * @return list of all service registries
     */
    List<ServiceRegistry> getRegisteredServices();

    /**
     * Retrieves a specific service by name.
     *
     * @param name the name of the service registry
     * @return the service registry details
     */
    ServiceRegistry getService(String name);

    /**
     * Creates or updates a service registry.
     * <p>
     * If a service with the given name already exists, it will be updated with the
     * new configuration.
     *
     * @param serviceRegistry the service registry configuration including name,
     *                        host, port, and other settings
     */
    void addOrUpdateService(ServiceRegistry serviceRegistry);

    /**
     * Removes a service from the registry.
     * <p>
     * This will delete the service configuration and all associated methods and
     * proto files.
     *
     * @param name the name of the service registry to remove
     */
    void removeService(String name);

    // Circuit breaker operations

    /**
     * Opens the circuit breaker for a service, preventing new requests.
     * <p>
     * When a circuit breaker is open, all requests to the service will immediately
     * fail
     * without attempting to connect. This is useful for temporarily disabling a
     * faulty service.
     *
     * @param name the name of the service registry
     * @return transition response with the new circuit breaker state
     */
    CircuitBreakerTransitionResponse openCircuitBreaker(String name);

    /**
     * Closes the circuit breaker for a service, allowing requests to proceed.
     * <p>
     * This re-enables a previously disabled service, allowing requests to flow
     * through again.
     *
     * @param name the name of the service registry
     * @return transition response with the new circuit breaker state
     */
    CircuitBreakerTransitionResponse closeCircuitBreaker(String name);

    /**
     * Retrieves the current circuit breaker status for a service.
     *
     * @param name the name of the service registry
     * @return the current circuit breaker state (OPEN, CLOSED, or HALF_OPEN)
     */
    CircuitBreakerTransitionResponse getCircuitBreakerStatus(String name);

    // Service method operations

    /**
     * Adds or updates a service method in the registry.
     * <p>
     * Service methods define the gRPC endpoints that can be called from workflows.
     *
     * @param registryName the name of the service registry
     * @param method       the service method configuration including method name,
     *                     request/response types, and options
     */
    void addOrUpdateServiceMethod(String registryName, ServiceMethod method);

    /**
     * Removes a specific method from a service.
     *
     * @param registryName the name of the service registry
     * @param serviceName  the name of the gRPC service (as defined in .proto file)
     * @param method       the method name to remove
     * @param methodType   the method type (e.g., UNARY, SERVER_STREAMING,
     *                     CLIENT_STREAMING, BIDI_STREAMING)
     */
    void removeMethod(String registryName, String serviceName, String method, String methodType);

    // Protocol Buffer (.proto) file operations

    /**
     * Retrieves the binary content of a proto file.
     * <p>
     * Proto files define the service interfaces and message types for gRPC
     * services.
     *
     * @param registryName the name of the service registry
     * @param filename     the name of the proto file (e.g., "myservice.proto")
     * @return the proto file content as raw bytes
     */
    byte[] getProtoData(String registryName, String filename);

    /**
     * Uploads or updates a proto file for a service.
     * <p>
     * After uploading a proto file, you can use the discover method to
     * automatically
     * extract service methods from it.
     *
     * @param registryName the name of the service registry
     * @param filename     the name of the proto file (e.g., "myservice.proto")
     * @param data         the proto file content as raw bytes
     */
    void setProtoData(String registryName, String filename, byte[] data);

    /**
     * Deletes a proto file from the service registry.
     *
     * @param registryName the name of the service registry
     * @param filename     the name of the proto file to delete
     */
    void deleteProto(String registryName, String filename);

    /**
     * Retrieves a list of all proto files registered for a service.
     *
     * @param registryName the name of the service registry
     * @return list of proto registry entries containing filename and metadata
     */
    List<ProtoRegistryEntry> getAllProtos(String registryName);

    // Discovery operations

    /**
     * Discovers service methods from uploaded proto files.
     * <p>
     * This automatically extracts all service methods defined in the proto files
     * associated with this service registry. If create is true, it will also
     * register these methods in the service registry.
     *
     * @param name   the name of the service registry
     * @param create if true, automatically creates service methods in the registry;
     *               if false, only returns discovered methods
     * @return list of discovered service methods
     */
    List<ServiceMethod> discover(String name, boolean create);
}
