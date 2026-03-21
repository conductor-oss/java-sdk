package aimodelevaluation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aimodelevaluation.workers.LoadModelWorker;
import aimodelevaluation.workers.PrepareTestSetWorker;
import aimodelevaluation.workers.RunInferenceWorker;
import aimodelevaluation.workers.ComputeMetricsWorker;
import aimodelevaluation.workers.ReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 802: AI Model Evaluation — Load Model, Prepare Test Set, Run Inference, Compute Metrics, Report
 */
public class AiModelEvaluationExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 802: AI Model Evaluation — Load Model, Prepare Test Set, Run Inference, Compute Metrics, Report ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("ame_load_model", "ame_prepare_test_set", "ame_run_inference", "ame_compute_metrics", "ame_report"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new LoadModelWorker(), new PrepareTestSetWorker(), new RunInferenceWorker(), new ComputeMetricsWorker(), new ReportWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("ame_model_evaluation", 1, Map.of("modelId", "MDL-ai-model-evaluation-BERT", "testDatasetId", "TEST-802", "taskType", "classification"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("reportId: %s%n", workflow.getOutput().get("reportId"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
