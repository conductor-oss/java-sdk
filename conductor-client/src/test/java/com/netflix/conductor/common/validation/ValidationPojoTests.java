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
package com.netflix.conductor.common.validation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationPojoTests {

    @Test
    public void testErrorResponseGettersSetters() {
        ErrorResponse errorResponse = new ErrorResponse();

        errorResponse.setStatus(400);
        errorResponse.setCode("VALIDATION_ERROR");
        errorResponse.setMessage("Invalid input provided");
        errorResponse.setInstance("/api/workflow");
        errorResponse.setRetryable(true);

        ValidationError validationError = new ValidationError("name", "must not be null", "null");
        List<ValidationError> validationErrors = Collections.singletonList(validationError);
        errorResponse.setValidationErrors(validationErrors);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("requestId", "abc-123");
        metadata.put("timestamp", 1234567890L);
        errorResponse.setMetadata(metadata);

        assertEquals(400, errorResponse.getStatus());
        assertEquals("VALIDATION_ERROR", errorResponse.getCode());
        assertEquals("Invalid input provided", errorResponse.getMessage());
        assertEquals("/api/workflow", errorResponse.getInstance());
        assertTrue(errorResponse.isRetryable());
        assertEquals(1, errorResponse.getValidationErrors().size());
        assertSame(validationErrors, errorResponse.getValidationErrors());
        assertEquals("abc-123", errorResponse.getMetadata().get("requestId"));
        assertEquals(1234567890L, errorResponse.getMetadata().get("timestamp"));
    }

    @Test
    public void testValidationErrorGettersSetters() {
        ValidationError error = new ValidationError();

        error.setPath("workflow.name");
        error.setMessage("must not be empty");
        error.setInvalidValue("");

        assertEquals("workflow.name", error.getPath());
        assertEquals("must not be empty", error.getMessage());
        assertEquals("", error.getInvalidValue());
    }

    @Test
    public void testValidationErrorConstructor() {
        ValidationError error = new ValidationError("task.type", "unsupported task type", "INVALID_TYPE");

        assertEquals("task.type", error.getPath());
        assertEquals("unsupported task type", error.getMessage());
        assertEquals("INVALID_TYPE", error.getInvalidValue());
    }

    @Test
    public void testValidationErrorToString() {
        ValidationError error = new ValidationError("field.path", "is required", "badValue");

        String result = error.toString();

        assertTrue(result.contains("field.path"), "toString should contain the path");
        assertTrue(result.contains("is required"), "toString should contain the message");
        assertTrue(result.contains("badValue"), "toString should contain the invalidValue");
        assertTrue(result.contains("ValidationError"), "toString should contain the class name");
    }
}
