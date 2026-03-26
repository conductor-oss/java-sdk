package apmworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import apmworkflow.workers.*;

import java.util.*;

/**
 * Example 421: APM Workflow
 *
 * Application Performance Monitoring: collect traces, analyze latency,
 * detect bottlenecks, and generate a report.
 *
 * Pattern:
 *   collect_traces -> analyze_latency -> detect_bottlenecks -> report
 */
public class ApmWorkflowExample {

    private static final List<String> TASK_NAMES = List.of(
            "apm_collect_traces",
            "apm_analyze_latency",
            "apm_detect_bottlenecks",
            "apm_report"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new CollectTraces(),
                new AnalyzeLatency(),
                new DetectBottlenecks(),
                new ApmReport()
        );
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 421: APM Workflow ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);

        System.out.println("Step 2: Registering workflow 'apm_workflow_421'...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = allWorkers();
        client.startWorkers(workers);

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting APM workflow...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("serviceName", "checkout-service");
        input.put("timeRange", "last-24h");
        input.put("percentile", "p99");

        String workflowId = client.startWorkflow("apm_workflow_421", 1, input);
        System.out.println("  Workflow ID: " + workflowId);

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        Map<String, Object> output = workflow.getOutput();

        System.out.println("\n  Trace Count: " + output.get("traceCount"));
        System.out.println("  P99 Latency: " + output.get("p99Latency") + "ms");
        System.out.println("  Bottlenecks Found: " + output.get("bottlenecksFound"));
        System.out.println("  Report Generated: " + output.get("reportGenerated"));

        client.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
