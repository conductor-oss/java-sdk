package visaprocessing;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import visaprocessing.workers.*;
import java.util.List; import java.util.Map;
public class VisaProcessingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 546: Visa Processing ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("vsp_collect","vsp_validate","vsp_submit","vsp_track","vsp_receive"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectWorker(),new ValidateWorker(),new SubmitWorker(),new TrackWorker(),new ReceiveWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("vsp_visa_processing", 1, Map.of("applicantId","TRV-300","country","Germany","visaType","business"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Application ID: " + wf.getOutput().get("applicationId"));
        System.out.println("  Visa status: " + wf.getOutput().get("visaStatus"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
