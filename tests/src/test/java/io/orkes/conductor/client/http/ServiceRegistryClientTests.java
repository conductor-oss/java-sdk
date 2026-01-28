/*
 * Copyright 2022 Conductor Authors.
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
package io.orkes.conductor.client.http;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.common.model.CircuitBreakerTransitionResponse;
import com.netflix.conductor.common.model.ProtoRegistryEntry;
import com.netflix.conductor.common.model.ServiceMethod;
import com.netflix.conductor.common.model.ServiceRegistry;

import io.orkes.conductor.client.ServiceRegistryClient;
import io.orkes.conductor.client.util.ClientTestUtil;

import lombok.SneakyThrows;

import static org.junit.jupiter.api.Assertions.assertNull;

public class ServiceRegistryClientTests {
    private static final String SERVICE_NAME = "test-sdk-java-service";
    private static final String SERVICE_URI = "localhost:50051";
    private static final String PROTO_FILENAME = "test-service.proto";

    private final ServiceRegistryClient serviceRegistryClient = ClientTestUtil.getOrkesClients()
            .getServiceRegistryClient();

    @BeforeEach
    void setup() {
        cleanup();
    }

    @AfterEach
    void cleanup() {
        try {
            serviceRegistryClient.removeService(SERVICE_NAME);
        } catch (ConductorClientException e) {
            // Ignore if service doesn't exist
            if (e.getStatus() != 404 && e.getStatus() != 500) {
                throw e;
            }
        }
    }

    @Test
    void testBasicCRUDOperations() {
        // Create a service registry
        ServiceRegistry service = new ServiceRegistry();
        service.setName(SERVICE_NAME);
        service.setType(ServiceRegistry.Type.gRPC);
        service.setServiceURI(SERVICE_URI);

        serviceRegistryClient.addOrUpdateService(service);

        // Get the service
        ServiceRegistry retrieved = serviceRegistryClient.getService(SERVICE_NAME);
        Assertions.assertNotNull(retrieved);
        Assertions.assertEquals(SERVICE_NAME, retrieved.getName());
        Assertions.assertEquals(SERVICE_URI, retrieved.getServiceURI());
        Assertions.assertEquals(ServiceRegistry.Type.gRPC, retrieved.getType());

        // Verify it appears in list
        List<ServiceRegistry> services = serviceRegistryClient.getRegisteredServices();
        Assertions.assertTrue(services.stream().anyMatch(s -> s.getName().equals(SERVICE_NAME)));

        // Update the service URI
        service.setServiceURI("grpc://localhost:50052");
        serviceRegistryClient.addOrUpdateService(service);

        ServiceRegistry updated = serviceRegistryClient.getService(SERVICE_NAME);
        Assertions.assertEquals("grpc://localhost:50052", updated.getServiceURI());

        // Delete the service
        serviceRegistryClient.removeService(SERVICE_NAME);

        // Verify deletion
        ServiceRegistry found = serviceRegistryClient.getService(SERVICE_NAME);
        assertNull(found);
    }

    // @Test
    // Disabled as this might change on the server side
    void testCircuitBreakerOperations() {
        // Create a service first
        ServiceRegistry service = createTestService();
        serviceRegistryClient.addOrUpdateService(service);

        // Get initial status
        CircuitBreakerTransitionResponse status = serviceRegistryClient.getCircuitBreakerStatus(SERVICE_NAME);
        Assertions.assertNotNull(status);

        // Open the circuit breaker
        CircuitBreakerTransitionResponse openResponse = serviceRegistryClient.openCircuitBreaker(SERVICE_NAME);
        Assertions.assertNotNull(openResponse);

        // Verify it's open
        status = serviceRegistryClient.getCircuitBreakerStatus(SERVICE_NAME);
        Assertions.assertNotNull(status);

        // Close the circuit breaker
        CircuitBreakerTransitionResponse closeResponse = serviceRegistryClient.closeCircuitBreaker(SERVICE_NAME);
        Assertions.assertNotNull(closeResponse);

        // Verify it's closed
        status = serviceRegistryClient.getCircuitBreakerStatus(SERVICE_NAME);
        Assertions.assertNotNull(status);
    }

    @Test
    void testProtoFileOperations() throws Exception {
        // Create a service first
        ServiceRegistry service = createTestService();
        serviceRegistryClient.addOrUpdateService(service);

        // Load proto file from test resources
        byte[] protoData = getClass().getClassLoader()
                .getResourceAsStream("compiled.bin")
                .readAllBytes();

        // Upload proto file
        serviceRegistryClient.setProtoData(SERVICE_NAME, PROTO_FILENAME, protoData);

        // Get all protos
        List<ProtoRegistryEntry> protos = serviceRegistryClient.getAllProtos(SERVICE_NAME);
        Assertions.assertNotNull(protos);
        Assertions.assertTrue(protos.stream().anyMatch(p -> p.getFilename().equals(PROTO_FILENAME)));

        // Get proto data
        byte[] retrievedData = serviceRegistryClient.getProtoData(SERVICE_NAME, PROTO_FILENAME);
        Assertions.assertNotNull(retrievedData);
        Assertions.assertTrue(retrievedData.length > 0);
        Assertions.assertEquals(protoData.length, retrievedData.length);

        // Delete proto
        serviceRegistryClient.deleteProto(SERVICE_NAME, PROTO_FILENAME);

        // Verify deletion
        List<ProtoRegistryEntry> remaining = serviceRegistryClient.getAllProtos(SERVICE_NAME);
        Assertions.assertTrue(remaining.stream().noneMatch(p -> p.getFilename().equals(PROTO_FILENAME)));
    }

    @Test
    void testServiceMethodOperations() {
        // Create a service first
        ServiceRegistry service = createTestService();
        serviceRegistryClient.addOrUpdateService(service);

        // Create a service method
        ServiceMethod method = new ServiceMethod();
        method.setOperationName("TestService.TestMethod");
        method.setMethodName("TestMethod");
        method.setMethodType("UNARY");
        method.setInputType("TestRequest");
        method.setOutputType("TestResponse");

        // Add method
        serviceRegistryClient.addOrUpdateServiceMethod(SERVICE_NAME, method);

        // Get the service to verify method was added
        ServiceRegistry retrieved = serviceRegistryClient.getService(SERVICE_NAME);
        Assertions.assertNotNull(retrieved);

        // Update the method
        method.setMethodType("SERVER_STREAMING");
        serviceRegistryClient.addOrUpdateServiceMethod(SERVICE_NAME, method);

        // Remove the method - serviceName parameter is part of the operation
        serviceRegistryClient.removeMethod(SERVICE_NAME, "TestService", "TestMethod", "SERVER_STREAMING");
    }

    @SneakyThrows
    // @Test
    // Disabled until we deploy a real server that can be used to discover services
    void testDiscoverMethods() {
        // Create a service first
        ServiceRegistry service = createTestService();
        service.setServiceURI("localhost:50053");
        serviceRegistryClient.addOrUpdateService(service);

        byte[] protoData = getClass().getClassLoader()
            .getResourceAsStream("compiled.bin")
            .readAllBytes();

        serviceRegistryClient.addOrUpdateService(service);
        serviceRegistryClient.setProtoData(SERVICE_NAME, PROTO_FILENAME, protoData);

        // Discover methods without creating them
        List<ServiceMethod> discovered = serviceRegistryClient.discover(SERVICE_NAME, false);
        Assertions.assertNotNull(discovered);
        // May be empty if proto parsing is not fully implemented

        // Discover and create methods
        List<ServiceMethod> created = serviceRegistryClient.discover(SERVICE_NAME, true);
        Assertions.assertNotNull(created);
    }

    @Test
    void testUpdateExistingService() {
        // Create initial service
        ServiceRegistry service = createTestService();
        serviceRegistryClient.addOrUpdateService(service);

        // Update with different configuration
        service.setServiceURI("grpc://localhost:50053");
        service.setCircuitBreakerEnabled(true);
        serviceRegistryClient.addOrUpdateService(service);

        // Verify update
        ServiceRegistry updated = serviceRegistryClient.getService(SERVICE_NAME);
        Assertions.assertEquals("grpc://localhost:50053", updated.getServiceURI());
        Assertions.assertTrue(updated.isCircuitBreakerEnabled());
    }

    private ServiceRegistry createTestService() {
        ServiceRegistry service = new ServiceRegistry();
        service.setName(SERVICE_NAME);
        service.setType(ServiceRegistry.Type.gRPC);
        service.setServiceURI(SERVICE_URI);
        return service;
    }
}
