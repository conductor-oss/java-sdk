package accountdeletion;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import accountdeletion.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 609: Account Deletion — Verify, Backup, Delete, Confirm
 */
public class AccountDeletionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 609: Account Deletion ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("acd_verify", "acd_backup", "acd_delete", "acd_confirm"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new VerifyDeletionWorker(), new BackupWorker(), new DeleteAccountWorker(), new ConfirmDeletionWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("acd_account_deletion", 1, Map.of("userId", "USR-DEL001", "reason", "no_longer_needed"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
