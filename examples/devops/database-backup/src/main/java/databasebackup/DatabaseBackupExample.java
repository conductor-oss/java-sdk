package databasebackup;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import databasebackup.workers.*;

import java.util.*;

/**
 * Database Backup — Snapshot, Compress, Upload, Verify
 *
 * Orchestrates a complete database backup pipeline: validates configuration,
 * takes a consistent snapshot, verifies integrity, uploads to offsite storage,
 * enforces retention policy, and sends a completion notification.
 *
 * Uses conductor-oss Java SDK v5 from https://github.com/conductor-oss/conductor/tree/main/conductor-clients
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/database-backup-1.0.0.jar
 */
public class DatabaseBackupExample {

    private static final List<String> TASK_NAMES = List.of(
            "backup_validate_config",
            "backup_take_snapshot",
            "backup_verify_integrity",
            "backup_upload_to_storage",
            "backup_cleanup_old",
            "backup_send_notification"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new ValidateConfig(),
                new TakeSnapshot(),
                new VerifyIntegrity(),
                new UploadToStorage(),
                new CleanupOldBackups(),
                new SendNotification()
        );
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Database Backup Demo: Snapshot, Compress, Upload, Verify ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'database_backup'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = allWorkers();
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode — workers are polling for tasks.");
            System.out.println("Use the Conductor CLI or UI to start workflows.\n");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join(); // Block forever
            return;
        }

        // Allow workers to start polling
        Thread.sleep(2000);

        // Step 4 — Run a database backup
        System.out.println("Step 4: Running database backup...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("database", Map.of(
                "type", "postgresql",
                "host", "db-primary.internal",
                "port", 5432,
                "name", "orders_production",
                "user", "backup_agent"
        ));
        input.put("storage", Map.of(
                "type", "s3",
                "bucket", "acme-db-backups",
                "path", "postgresql/orders",
                "region", "us-east-1"
        ));
        input.put("retention", Map.of(
                "days", 30,
                "maxBackups", 10
        ));
        input.put("notification", Map.of(
                "channel", "slack",
                "recipients", List.of("dba-team@example.com", "oncall@example.com")
        ));

        String workflowId = client.startWorkflow("database_backup", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for workflow to complete...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        Map<String, Object> output = workflow.getOutput();
        if (output != null) {
            System.out.println("--- Backup Results ---");
            System.out.println("  Database     : " + output.get("databaseName"));
            System.out.println("  File         : " + output.get("filename"));
            System.out.println("  Size         : " + output.get("sizeFormatted"));
            System.out.println("  Storage      : " + output.get("storageUri"));
            System.out.println("  Verified     : " + output.get("verified"));
            System.out.println("  Old deleted  : " + output.get("deletedCount"));
            System.out.println("  Space freed  : " + output.get("freedFormatted"));
            System.out.println("  Notification : " + output.get("notificationStatus"));
        }

        client.stopWorkers();

        if (!"COMPLETED".equals(status)) {
            System.out.println("\nWorkflow did not complete (status: " + status + ")");
            workflow.getTasks().stream()
                    .filter(t -> t.getStatus().name().equals("FAILED"))
                    .forEach(t -> System.out.println("  Failed task: " + t.getReferenceTaskName()
                            + " — " + t.getReasonForIncompletion()));
            System.out.println("Result: BACKUP_FAILED");
            System.exit(1);
        }

        boolean verified = output != null && Boolean.TRUE.equals(output.get("verified"));
        if (verified) {
            System.out.println("\nResult: BACKUP_SUCCESS — verified and uploaded");
            System.exit(0);
        } else {
            System.out.println("\nResult: BACKUP_UNVERIFIED — completed but integrity check failed");
            System.exit(1);
        }
    }
}
