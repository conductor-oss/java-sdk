package llmchain;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import llmchain.workers.ChainGenerateWorker;
import llmchain.workers.ChainParseWorker;
import llmchain.workers.ChainPromptWorker;
import llmchain.workers.ChainValidateWorker;

import java.util.List;
import java.util.Map;

/**
 * LLM Chain — Chained workers forming an LLM pipeline:
 * prompt -> generate -> parse -> validate
 *
 * Run:
 *   java -jar target/llm-chain-1.0.0.jar
 */
public class LlmChainExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== LLM Chain: Chained LLM Pipeline ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "chain_prompt", "chain_generate", "chain_parse", "chain_validate"));
        System.out.println("  Registered: chain_prompt, chain_generate, chain_parse, chain_validate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'llm_chain_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ChainPromptWorker(),
                new ChainGenerateWorker(),
                new ChainParseWorker(),
                new ChainValidateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("llm_chain_workflow", 1, Map.of(
                "customerEmail", "Subject: Frustrated with recent service\n\nI have been a loyal customer for 3 years but the recent downtime has severely impacted our operations. We need enterprise-grade reliability and dedicated support. Please advise on upgrade options.",
                "productCatalog", "PROD-BASIC-100,PROD-PRO-250,PROD-ENT-500,PROD-SUPPORT-PREM,PROD-SUPPORT-STD"
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
