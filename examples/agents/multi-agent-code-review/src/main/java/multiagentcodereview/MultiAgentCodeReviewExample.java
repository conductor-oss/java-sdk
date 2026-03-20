package multiagentcodereview;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multiagentcodereview.workers.ParseCodeWorker;
import multiagentcodereview.workers.SecurityReviewWorker;
import multiagentcodereview.workers.PerformanceReviewWorker;
import multiagentcodereview.workers.StyleReviewWorker;
import multiagentcodereview.workers.CompileReviewWorker;

import java.util.List;
import java.util.Map;

/**
 * Multi-Agent Code Review Demo
 *
 * Demonstrates a pattern where code is parsed, then three specialized review
 * agents (security, performance, style) analyze it in parallel via FORK/JOIN,
 * and a compile-review agent aggregates the findings into a final report.
 *
 * Run:
 *   java -jar target/multi-agent-code-review-1.0.0.jar
 */
public class MultiAgentCodeReviewExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Agent Code Review Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cr_parse_code", "cr_security_review", "cr_performance_review",
                "cr_style_review", "cr_compile_review"));
        System.out.println("  Registered: cr_parse_code, cr_security_review, cr_performance_review, cr_style_review, cr_compile_review\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'multi_agent_code_review'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ParseCodeWorker(),
                new SecurityReviewWorker(),
                new PerformanceReviewWorker(),
                new StyleReviewWorker(),
                new CompileReviewWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("multi_agent_code_review", 1,
                Map.of("code", "const express = require('express');\nconst crypto = require('crypto');\n// ... application code",
                        "language", "javascript"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
