package propertyvaluation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import propertyvaluation.workers.*;
import java.util.List;
import java.util.Map;
public class PropertyValuationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 682: Property Valuation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("pvl_collect_comps","pvl_analyze","pvl_appraise","pvl_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectCompsWorker(),new AnalyzeWorker(),new AppraiseWorker(),new ReportWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("pvl_property_valuation",1,Map.of("propertyId","PROP-200","address","123 Oak Lane, Austin TX"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status);
        System.out.println("  Estimated value: "+wf.getOutput().get("estimatedValue"));
        System.out.println("  Report ID: "+wf.getOutput().get("reportId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
