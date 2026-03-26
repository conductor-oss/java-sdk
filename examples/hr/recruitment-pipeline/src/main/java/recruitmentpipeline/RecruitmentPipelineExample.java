package recruitmentpipeline;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import recruitmentpipeline.workers.*;
import java.util.List; import java.util.Map;
public class RecruitmentPipelineExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 602: Recruitment Pipeline ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rcp_post","rcp_screen","rcp_interview","rcp_evaluate","rcp_offer"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new PostWorker(),new ScreenWorker(),new InterviewWorker(),new EvaluateWorker(),new OfferWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("rcp_recruitment_pipeline", 1,
                Map.of("jobTitle","Senior Software Engineer","department","Engineering","candidateName","Alex Rivera"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Recommendation: " + wf.getOutput().get("recommendation"));
        System.out.println("  Offer ID: " + wf.getOutput().get("offerId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
