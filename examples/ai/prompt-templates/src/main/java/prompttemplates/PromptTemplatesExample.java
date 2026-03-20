package prompttemplates;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import prompttemplates.workers.CallLlmWorker;
import prompttemplates.workers.CollectWorker;
import prompttemplates.workers.ResolveTemplateWorker;

import java.util.List;
import java.util.Map;

/**
 * Prompt Templates — Resolve versioned prompt templates, call an LLM, and collect results.
 *
 * Demonstrates a three-step AI pipeline:
 *   1. pt_resolve_template — looks up a versioned template and substitutes variables
 *   2. pt_call_llm — sends the resolved prompt to a (deterministic. LLM
 *   3. pt_collect — logs the final result
 *
 * Run:
 *   java -jar target/prompt-templates-1.0.0.jar
 *   java -jar target/prompt-templates-1.0.0.jar --workers
 */
public class PromptTemplatesExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Prompt Templates: Versioned Prompt Resolution Pipeline ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("pt_resolve_template", "pt_call_llm", "pt_collect"));
        System.out.println("  Registered: pt_resolve_template, pt_call_llm, pt_collect\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'prompt_templates_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ResolveTemplateWorker(),
                new CallLlmWorker(),
                new CollectWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("prompt_templates_workflow", 1,
                Map.of(
                        "templateId", "summarize",
                        "templateVersion", 2,
                        "variables", Map.of(
                                "format", "technical document",
                                "topic", "Conductor Orchestration",
                                "audience", "developers",
                                "length", "concise"
                        ),
                        "model", "gpt-4"
                ));
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
