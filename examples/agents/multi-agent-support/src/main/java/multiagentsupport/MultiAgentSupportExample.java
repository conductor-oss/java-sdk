package multiagentsupport;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multiagentsupport.workers.ClassifyTicketWorker;
import multiagentsupport.workers.KnowledgeSearchWorker;
import multiagentsupport.workers.SolutionProposeWorker;
import multiagentsupport.workers.FeatureEvaluateWorker;
import multiagentsupport.workers.GeneralRespondWorker;
import multiagentsupport.workers.QaValidateWorker;

import java.util.List;
import java.util.Map;

/**
 * Multi-Agent Customer Support Demo
 *
 * Demonstrates a multi-agent pattern where a classifier routes support tickets
 * to specialized agents: bug fix (knowledge search + solution propose),
 * feature evaluation, or general response. All paths converge on a QA
 * validation step.
 *
 * Run:
 *   java -jar target/multi-agent-support-1.0.0.jar
 */
public class MultiAgentSupportExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Agent Customer Support Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cs_classify_ticket", "cs_knowledge_search", "cs_solution_propose",
                "cs_feature_evaluate", "cs_general_respond", "cs_qa_validate"));
        System.out.println("  Registered: cs_classify_ticket, cs_knowledge_search, cs_solution_propose, cs_feature_evaluate, cs_general_respond, cs_qa_validate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'multi_agent_customer_support'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ClassifyTicketWorker(),
                new KnowledgeSearchWorker(),
                new SolutionProposeWorker(),
                new FeatureEvaluateWorker(),
                new GeneralRespondWorker(),
                new QaValidateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("multi_agent_customer_support", 1,
                Map.of("ticketId", "TKT-9182",
                        "subject", "Application crashes on login",
                        "description", "The app crashes with error code 500 when attempting to log in after the latest update.",
                        "customerTier", "premium"));
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
