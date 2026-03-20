package reverselogistics;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import reverselogistics.workers.*;
import java.util.*;
public class ReverseLogisticsExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 669: Reverse Logistics ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rvl_receive_return","rvl_inspect","rvl_refurbish","rvl_recycle","rvl_dispose","rvl_process"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new ReceiveReturnWorker(), new InspectWorker(), new RefurbishWorker(), new RecycleWorker(), new DisposeWorker(), new ProcessWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("rvl_reverse_logistics", 1,
                Map.of("returnId","RET-2024-669","product","Wireless Headphones","reason","defective_speaker"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
