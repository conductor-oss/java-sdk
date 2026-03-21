package featureengineering;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import featureengineering.workers.*;

import java.util.List;
import java.util.Map;

public class FeatureEngineeringExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Feature Engineering Demo ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of("fe_extract_features", "fe_transform_features", "fe_normalize_features", "fe_validate_features"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new ExtractFeaturesWorker(), new TransformFeaturesWorker(), new NormalizeFeaturesWorker(), new ValidateFeaturesWorker());
        client.startWorkers(workers);

        if (workersOnly) { System.out.println("Worker-only mode. Ctrl+C to stop."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("feature_engineering", 1,
                Map.of("rawData", List.of(
                        Map.of("age", 35, "income", 75000, "tenure_months", 48, "num_products", 3, "has_credit_card", true, "is_active", true, "balance", 12000),
                        Map.of("age", 28, "income", 55000, "tenure_months", 12, "num_products", 1, "has_credit_card", false, "is_active", true, "balance", 3000)),
                "featureConfig", Map.of("method", "min-max")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("Status: " + wf.getStatus().name() + "\nOutput: " + wf.getOutput());
        client.stopWorkers();
    }
}
