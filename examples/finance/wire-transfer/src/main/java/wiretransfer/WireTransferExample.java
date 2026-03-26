package wiretransfer;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import wiretransfer.workers.*;
import java.util.List;
import java.util.Map;

public class WireTransferExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 498: Wire Transfer ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("wir_validate", "wir_verify_sender", "wir_compliance_check", "wir_execute", "wir_confirm"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ValidateWorker(), new VerifySenderWorker(), new ComplianceCheckWorker(), new ExecuteWorker(), new ConfirmWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("wire_transfer_workflow", 1, Map.of("transferId", "WIRE-2024-0891", "senderAccount", "ACCT-1001-USD", "recipientAccount", "ACCT-EXT-5501-USD", "amount", 75000, "currency", "USD"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
