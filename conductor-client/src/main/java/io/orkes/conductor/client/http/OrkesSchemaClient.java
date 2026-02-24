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

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.common.metadata.SchemaDef;

import io.orkes.conductor.client.SchemaClient;

public class OrkesSchemaClient implements SchemaClient {

    private final SchemaResource schemaResource;

    public OrkesSchemaClient(ConductorClient client) {
        this.schemaResource = new SchemaResource(client);
    }

    @Override
    public void saveSchema(SchemaDef schemaDef) {
        schemaResource.saveSchemas(List.of(schemaDef));
    }

    @Override
    public void saveSchemas(List<SchemaDef> schemaDefs) {
        schemaResource.saveSchemas(schemaDefs);
    }

    @Override
    public List<SchemaDef> getAllSchemas(Boolean shortFormat) {
        return schemaResource.getAllSchemas(shortFormat);
    }

    @Override
    public SchemaDef getSchema(String name) {
        return schemaResource.getSchema(name);
    }

    @Override
    public SchemaDef getSchema(String name, int version) {
        return schemaResource.getSchema(name, version);
    }

    @Override
    public void deleteSchema(String name) {
        schemaResource.deleteSchema(name);
    }

    @Override
    public void deleteSchema(String name, int version) {
        schemaResource.deleteSchema(name, version);
    }
}
