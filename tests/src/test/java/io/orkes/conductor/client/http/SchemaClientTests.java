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
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.common.metadata.SchemaDef;

import io.orkes.conductor.client.SchemaClient;
import io.orkes.conductor.client.util.ClientTestUtil;

public class SchemaClientTests {

    private static final String SCHEMA_NAME = "test-sdk-java-schema";

    private final SchemaClient schemaClient = ClientTestUtil.getOrkesClients().getSchemaClient();

    @Test
    void testMethods() {
        // 1. Delete schema if it already exists (swallow 404/500)
        try {
            schemaClient.deleteSchema(SCHEMA_NAME);
        } catch (ConductorClientException e) {
            if (e.getStatus() != 404 && e.getStatus() != 500) {
                throw e;
            }
        }

        // 2. Save a single JSON schema
        SchemaDef schema = SchemaDef.builder()
                .name(SCHEMA_NAME)
                .version(1)
                .type(SchemaDef.Type.JSON)
                .data(Map.of("type", "object", "properties", Map.of("id", Map.of("type", "string"))))
                .build();
        schemaClient.saveSchema(schema);

        // 3. Get by name and assert fields
        SchemaDef fetched = schemaClient.getSchema(SCHEMA_NAME);
        Assertions.assertNotNull(fetched);
        Assertions.assertEquals(SCHEMA_NAME, fetched.getName());
        Assertions.assertEquals(SchemaDef.Type.JSON, fetched.getType());

        // 4. Get by name + version and assert
        SchemaDef fetchedByVersion = schemaClient.getSchema(SCHEMA_NAME, 1);
        Assertions.assertNotNull(fetchedByVersion);
        Assertions.assertEquals(1, fetchedByVersion.getVersion());

        // 5. Get all schemas and assert name is present
        List<SchemaDef> all = schemaClient.getAllSchemas(false);
        Assertions.assertNotNull(all);
        Assertions.assertTrue(all.stream().anyMatch(s -> SCHEMA_NAME.equals(s.getName())));

        // 6. Save schemas (bulk save path)
        SchemaDef schemaV2 = SchemaDef.builder()
                .name(SCHEMA_NAME)
                .version(2)
                .type(SchemaDef.Type.JSON)
                .data(Map.of("type", "object", "properties", Map.of("id", Map.of("type", "string"), "name", Map.of("type", "string"))))
                .build();
        schemaClient.saveSchemas(List.of(schemaV2));

        SchemaDef fetchedV2 = schemaClient.getSchema(SCHEMA_NAME, 2);
        Assertions.assertNotNull(fetchedV2);
        Assertions.assertEquals(2, fetchedV2.getVersion());

        // 7. Delete specific version and clean up
        schemaClient.deleteSchema(SCHEMA_NAME, 2);
        schemaClient.deleteSchema(SCHEMA_NAME, 1);
    }
}
