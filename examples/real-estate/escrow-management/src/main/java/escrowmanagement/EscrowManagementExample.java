package escrowmanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import escrowmanagement.workers.*;
import java.util.List;
import java.util.Map;
public class EscrowManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 689: Escrow Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("esc_open","esc_deposit","esc_verify","esc_release","esc_close"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new OpenEscrowWorker(),new DepositWorker(),new VerifyWorker(),new ReleaseWorker(),new CloseEscrowWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("esc_escrow_management",1,Map.of("buyerId","BUY-100","sellerId","SEL-200","amount",475000));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Escrow ID: "+wf.getOutput().get("escrowId")+"\n  Closing: "+wf.getOutput().get("closingStatus"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
