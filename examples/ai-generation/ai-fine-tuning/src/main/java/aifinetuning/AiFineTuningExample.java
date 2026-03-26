package aifinetuning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aifinetuning.workers.PrepareDatasetWorker;
import aifinetuning.workers.ConfigureWorker;
import aifinetuning.workers.TrainWorker;
import aifinetuning.workers.EvaluateWorker;
import aifinetuning.workers.DeployWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 804: AI Fine-Tuning — Prepare Dataset, Configure, Train, Evaluate, Deploy
 */
public class AiFineTuningExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 804: AI Fine-Tuning — Prepare Dataset, Configure, Train, Evaluate, Deploy ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("aft_prepare_dataset", "aft_configure", "aft_train", "aft_evaluate", "aft_deploy"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new PrepareDatasetWorker(), new ConfigureWorker(), new TrainWorker(), new EvaluateWorker(), new DeployWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("aft_fine_tuning", 1, Map.of("baseModel", "llama-3-8b", "datasetId", "DS-804", "taskType", "sentiment-analysis"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("modelId: %s%n", workflow.getOutput().get("modelId"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
