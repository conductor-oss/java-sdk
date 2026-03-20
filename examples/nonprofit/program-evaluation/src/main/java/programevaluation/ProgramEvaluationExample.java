package programevaluation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import programevaluation.workers.*;
import java.util.List;
import java.util.Map;
/** Example 757: Program Evaluation — Define Metrics, Collect, Analyze, Benchmark, Recommend */
public class ProgramEvaluationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 757: Program Evaluation ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("pev_define_metrics", "pev_collect", "pev_analyze", "pev_benchmark", "pev_recommend"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DefineMetricsWorker(), new CollectWorker(), new AnalyzeWorker(), new BenchmarkWorker(), new RecommendWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("program_evaluation_757", 1, Map.of("programName", "Youth Mentorship", "evaluationPeriod", "Q4-2025"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
