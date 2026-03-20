package mldatapipeline;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import mldatapipeline.workers.*;
import java.util.List;
import java.util.Map;

public class MlDataPipelineExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== ML Data Pipeline Demo ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ml_collect_data", "ml_clean_data", "ml_split_data", "ml_train_model", "ml_evaluate_model"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectDataWorker(), new CleanDataWorker(), new SplitDataWorker(), new TrainModelWorker(), new EvaluateModelWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode. Ctrl+C to stop."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("ml_data_pipeline", 1, Map.of("dataSource", Map.of("name", "iris_dataset"), "modelType", "random_forest", "splitRatio", 0.8));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("Status: " + wf.getStatus().name() + "\nOutput: " + wf.getOutput());
        client.stopWorkers();
    }
}
