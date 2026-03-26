package abtesting;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import abtesting.workers.DefineVariantsWorker;
import abtesting.workers.AssignUsersWorker;
import abtesting.workers.CollectDataWorker;
import abtesting.workers.AnalyzeResultsWorker;
import abtesting.workers.DecideWinnerWorker;
import java.util.List;
import java.util.Map;
public class AbTestingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 518: A/B Testing ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("abt_define_variants", "abt_assign_users", "abt_collect_data", "abt_analyze_results", "abt_decide_winner"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DefineVariantsWorker(), new AssignUsersWorker(), new CollectDataWorker(), new AnalyzeResultsWorker(), new DecideWinnerWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("ab_testing_workflow", 1, Map.of("testId", "ABT-518-001", "testName", "Checkout Button Color", "variantA", "Blue Button", "variantB", "Green Button", "sampleSize", 10000));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
