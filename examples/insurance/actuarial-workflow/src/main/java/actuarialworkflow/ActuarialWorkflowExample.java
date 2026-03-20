package actuarialworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import actuarialworkflow.workers.CollectDataWorker;
import actuarialworkflow.workers.ModelWorker;
import actuarialworkflow.workers.RunAnalysisWorker;
import actuarialworkflow.workers.AnalyzeWorker;
import actuarialworkflow.workers.ReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 706: Actuarial Workflow
 */
public class ActuarialWorkflowExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 706: Actuarial Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("act_collect_data", "act_model", "act_run_analysis", "act_analyze", "act_report"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectDataWorker(), new ModelWorker(), new RunAnalysisWorker(), new AnalyzeWorker(), new ReportWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("act_actuarial_workflow", 1, Map.of("lineOfBusiness", "commercial-property", "analysisYear", "2024", "modelType", "monte-carlo"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("reportId: %s%n", workflow.getOutput().get("reportId"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
