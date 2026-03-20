package supplierevaluation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import supplierevaluation.workers.*;
import java.util.*;

public class SupplierEvaluationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 661: Supplier Evaluation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("spe_collect_data","spe_score","spe_rank","spe_report"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectDataWorker(), new ScoreWorker(), new RankWorker(), new ReportWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("spe_supplier_evaluation", 1, Map.of("category","raw-materials","period","Q4-2024"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
