package databasebackup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Validates the backup configuration before starting the pipeline.
 * Checks that required fields (database host, port, name, credentials,
 * storage destination) are present and well-formed. Rejects invalid
 * configurations early so downstream workers never run with bad input.
 */
public class ValidateConfig implements Worker {

    private static final Set<String> SUPPORTED_DB_TYPES = Set.of(
            "postgresql", "mysql", "mongodb", "redis"
    );

    private static final Set<String> SUPPORTED_STORAGE_TYPES = Set.of(
            "s3", "gcs", "azure-blob", "local"
    );

    @Override
    public String getTaskDefName() {
        return "backup_validate_config";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[backup_validate_config] Validating backup configuration...");

        TaskResult result = new TaskResult(task);
        Map<String, Object> dbConfig = (Map<String, Object>) task.getInputData().get("database");
        Map<String, Object> storageConfig = (Map<String, Object>) task.getInputData().get("storage");
        Map<String, Object> retentionConfig = (Map<String, Object>) task.getInputData().get("retention");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Validate database configuration
        if (dbConfig == null) {
            errors.add("Missing required field: database");
        } else {
            String host = (String) dbConfig.get("host");
            if (host == null || host.isBlank()) {
                errors.add("database.host is required");
            }

            Object portObj = dbConfig.get("port");
            if (portObj == null) {
                errors.add("database.port is required");
            } else {
                int port = toInt(portObj, -1);
                if (port < 1 || port > 65535) {
                    errors.add("database.port must be between 1 and 65535, got: " + portObj);
                }
            }

            String dbName = (String) dbConfig.get("name");
            if (dbName == null || dbName.isBlank()) {
                errors.add("database.name is required");
            }

            String dbType = (String) dbConfig.get("type");
            if (dbType == null || dbType.isBlank()) {
                errors.add("database.type is required");
            } else if (!SUPPORTED_DB_TYPES.contains(dbType.toLowerCase())) {
                errors.add("database.type '" + dbType + "' is not supported. Supported: " + SUPPORTED_DB_TYPES);
            }

            String user = (String) dbConfig.get("user");
            if (user == null || user.isBlank()) {
                warnings.add("database.user is not set — will attempt connection without authentication");
            }
        }

        // Validate storage configuration
        if (storageConfig == null) {
            errors.add("Missing required field: storage");
        } else {
            String storageType = (String) storageConfig.get("type");
            if (storageType == null || storageType.isBlank()) {
                errors.add("storage.type is required");
            } else if (!SUPPORTED_STORAGE_TYPES.contains(storageType.toLowerCase())) {
                errors.add("storage.type '" + storageType + "' is not supported. Supported: " + SUPPORTED_STORAGE_TYPES);
            }

            String bucket = (String) storageConfig.get("bucket");
            String path = (String) storageConfig.get("path");
            if ((bucket == null || bucket.isBlank()) && (path == null || path.isBlank())) {
                errors.add("storage.bucket or storage.path is required");
            }
        }

        // Validate retention configuration (optional, apply defaults)
        int retentionDays = 30;
        int maxBackups = 10;
        if (retentionConfig != null) {
            retentionDays = toInt(retentionConfig.get("days"), 30);
            maxBackups = toInt(retentionConfig.get("maxBackups"), 10);
            if (retentionDays < 1) {
                errors.add("retention.days must be at least 1");
            }
            if (maxBackups < 1) {
                errors.add("retention.maxBackups must be at least 1");
            }
        } else {
            warnings.add("No retention config provided — using defaults (30 days, 10 max backups)");
        }

        boolean valid = errors.isEmpty();

        if (valid) {
            System.out.println("  Configuration valid");
            if (!warnings.isEmpty()) {
                warnings.forEach(w -> System.out.println("  WARNING: " + w));
            }
        } else {
            System.out.println("  Configuration INVALID — " + errors.size() + " error(s)");
            errors.forEach(e -> System.out.println("  ERROR: " + e));
        }

        result.getOutputData().put("valid", valid);
        result.getOutputData().put("errors", errors);
        result.getOutputData().put("warnings", warnings);
        result.getOutputData().put("retentionDays", retentionDays);
        result.getOutputData().put("maxBackups", maxBackups);

        if (valid) {
            String dbType = ((String) dbConfig.get("type")).toLowerCase();
            result.getOutputData().put("databaseType", dbType);
            result.getOutputData().put("databaseHost", dbConfig.get("host"));
            result.getOutputData().put("databaseName", dbConfig.get("name"));
            result.getOutputData().put("storageType", ((String) storageConfig.get("type")).toLowerCase());
            result.setStatus(TaskResult.Status.COMPLETED);
        } else {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Invalid backup configuration: " + String.join("; ", errors));
        }
        return result;
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultVal; }
    }
}
