package chainofthought;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import chainofthought.workers.UnderstandProblemWorker;
import chainofthought.workers.Step1ReasonWorker;
import chainofthought.workers.Step2CalculateWorker;
import chainofthought.workers.Step3VerifyWorker;
import chainofthought.workers.FinalAnswerWorker;

import java.util.List;
import java.util.Map;

/**
 * Chain of Thought Demo
 *
 * Demonstrates a sequential pipeline of five workers that solve a problem
 * through structured reasoning:
 *   ct_understand_problem -> ct_step_1_reason -> ct_step_2_calculate
 *       -> ct_step_3_verify -> ct_final_answer
 *
 * Run:
 *   java -jar target/chain-of-thought-1.0.0.jar
 */
public class ChainOfThoughtExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Chain of Thought Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ct_understand_problem", "ct_step_1_reason",
                "ct_step_2_calculate", "ct_step_3_verify",
                "ct_final_answer"));
        System.out.println("  Registered: ct_understand_problem, ct_step_1_reason, ct_step_2_calculate, ct_step_3_verify, ct_final_answer\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'chain_of_thought'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new UnderstandProblemWorker(),
                new Step1ReasonWorker(),
                new Step2CalculateWorker(),
                new Step3VerifyWorker(),
                new FinalAnswerWorker()
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
        String workflowId = client.startWorkflow("chain_of_thought", 1,
                Map.of("problem", "What is the compound interest on $10,000 at 5% annual rate for 3 years?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
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
