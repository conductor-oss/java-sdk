# SchemaClient

The `SchemaClient` provides operations to manage schemas in Conductor. Schemas define the structure of workflow inputs, outputs, and task data using JSON Schema, Avro, or Protobuf formats.

## Setup

```java
import com.netflix.conductor.client.http.ConductorClient;
import io.orkes.conductor.client.OrkesClients;
import io.orkes.conductor.client.SchemaClient;

ConductorClient client = new ConductorClient("https://your-conductor-server/api");
OrkesClients orkesClients = new OrkesClients(client);
SchemaClient schemaClient = orkesClients.getSchemaClient();
```

## Save a Schema

```java
import com.netflix.conductor.common.metadata.SchemaDef;
import java.util.Map;

SchemaDef schema = SchemaDef.builder()
        .name("my-schema")
        .version(1)
        .type(SchemaDef.Type.JSON)
        .data(Map.of(
                "type", "object",
                "properties", Map.of(
                        "id",   Map.of("type", "string"),
                        "name", Map.of("type", "string")
                )
        ))
        .build();

schemaClient.saveSchema(schema);
```

## Save Multiple Schemas (bulk)

```java
schemaClient.saveSchemas(List.of(schemaA, schemaB));
```

## Get a Schema (latest version)

```java
SchemaDef schema = schemaClient.getSchema("my-schema");
```

## Get a Specific Version

```java
SchemaDef schema = schemaClient.getSchema("my-schema", 2);
```

## List All Schemas

```java
// Full details
List<SchemaDef> all = schemaClient.getAllSchemas(false);

// Short format (name + version only)
List<SchemaDef> summary = schemaClient.getAllSchemas(true);
```

## Delete a Schema

```java
// Delete all versions
schemaClient.deleteSchema("my-schema");

// Delete a specific version
schemaClient.deleteSchema("my-schema", 2);
```

## Versioning

Every `SchemaDef` carries an integer `version` field (default `1`). To create a new version of an existing schema, save a `SchemaDef` with the same `name` and an incremented `version`.

```java
SchemaDef v2 = SchemaDef.builder()
        .name("my-schema")
        .version(2)
        .type(SchemaDef.Type.JSON)
        .data(updatedData)
        .build();

schemaClient.saveSchema(v2);
```

## Schema Types

| Type | Description |
|------|-------------|
| `JSON` | JSON Schema format |
| `AVRO` | Apache Avro schema |
| `PROTOBUF` | Protocol Buffers schema |
