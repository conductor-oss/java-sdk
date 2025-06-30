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
package com.netflix.conductor.client.http;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.type.TypeReference;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ConductorClientBinaryDataTest {

    @Test
    public void testBinaryDataDeserializationForByteArray() throws IOException {
        // Create test binary data
        byte[] testData = "test binary data".getBytes();
        
        // Mock the Response and ResponseBody
        ResponseBody responseBody = ResponseBody.create(testData, MediaType.parse("application/octet-stream"));
        
        Request request = new Request.Builder()
                .url("http://localhost:8080/test")
                .build();
                
        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Type", "application/octet-stream")
                .body(responseBody)
                .build();

        // Create a ConductorClient instance
        ConductorClient client = new ConductorClient("http://localhost:8080");
        
        // Use reflection to call the private deserialize method
        try {
            java.lang.reflect.Method deserializeMethod = ConductorClient.class.getDeclaredMethod("deserialize", Response.class, java.lang.reflect.Type.class);
            deserializeMethod.setAccessible(true);
            
            byte[] result = (byte[]) deserializeMethod.invoke(client, response, byte[].class);
            
            assertNotNull(result);
            assertArrayEquals(testData, result);
            
        } catch (Exception e) {
            fail("Failed to test deserialize method: " + e.getMessage());
        } finally {
            client.shutdown();
        }
    }
    
    @Test
    public void testHandleResponseForBinaryData() throws IOException {
        // Create test binary data
        byte[] testData = "test proto data".getBytes();
        
        // Mock the ResponseBody
        ResponseBody responseBody = ResponseBody.create(testData, MediaType.parse("application/octet-stream"));
        
        Request request = new Request.Builder()
                .url("http://localhost:8080/test")
                .build();
                
        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Type", "application/octet-stream")
                .body(responseBody)
                .build();

        // Create a ConductorClient instance
        ConductorClient client = new ConductorClient("http://localhost:8080");
        
        // Use reflection to call the protected handleResponse method
        try {
            java.lang.reflect.Method handleResponseMethod = ConductorClient.class.getDeclaredMethod("handleResponse", Response.class, java.lang.reflect.Type.class);
            handleResponseMethod.setAccessible(true);
            
            byte[] result = (byte[]) handleResponseMethod.invoke(client, response, byte[].class);
            
            assertNotNull(result);
            assertArrayEquals(testData, result);
            
        } catch (Exception e) {
            fail("Failed to test handleResponse method: " + e.getMessage());
        } finally {
            client.shutdown();
        }
    }
}
