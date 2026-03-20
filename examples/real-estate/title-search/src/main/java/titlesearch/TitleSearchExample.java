package titlesearch;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import titlesearch.workers.*;
import java.util.List;
import java.util.Map;
public class TitleSearchExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 690: Title Search ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tts_search","tts_verify_ownership","tts_check_liens","tts_certify"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new SearchRecordsWorker(),new VerifyOwnershipWorker(),new CheckLiensWorker(),new CertifyTitleWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("tts_title_search",1,Map.of("propertyId","PROP-500","address","123 Oak Lane, Austin TX","county","Travis"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Title: "+wf.getOutput().get("titleStatus")+"\n  Certificate: "+wf.getOutput().get("certificateId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
