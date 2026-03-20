package ragknowledgegraph;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragknowledgegraph.workers.ExtractEntitiesWorker;
import ragknowledgegraph.workers.GraphTraverseWorker;
import ragknowledgegraph.workers.VectorSearchWorker;
import ragknowledgegraph.workers.MergeContextWorker;
import ragknowledgegraph.workers.GenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 155: Knowledge Graph RAG
 *
 * Extracts entities from the question, then runs graph traversal and vector
 * search in parallel via FORK/JOIN, merges the results, and generates a
 * final answer enriched by both knowledge graph facts and vector documents.
 *
 * Pattern:
 *   extract_entities -> FORK -+-> graph_traverse ->+-> JOIN -> merge_context -> generate
 *                             +-> vector_search  ->+
 *
 * Run:
 *   java -jar target/rag-knowledge-graph-1.0.0.jar
 *   java -jar target/rag-knowledge-graph-1.0.0.jar --workers
 */
public class RagKnowledgeGraphExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 155: Knowledge Graph RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 - Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "kg_extract_entities", "kg_graph_traverse",
                "kg_vector_search", "kg_merge_context", "kg_generate"));
        System.out.println("  Registered: kg_extract_entities, kg_graph_traverse, kg_vector_search, kg_merge_context, kg_generate\n");

        // Step 2 - Register workflow
        System.out.println("Step 2: Registering workflow 'rag_knowledge_graph_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 - Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ExtractEntitiesWorker(),
                new GraphTraverseWorker(),
                new VectorSearchWorker(),
                new MergeContextWorker(),
                new GenerateWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 - Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rag_knowledge_graph_workflow", 1,
                Map.of("question", "What is Conductor and how does it relate to Netflix and microservices?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 - Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();

        System.out.println("  Question: " + workflow.getOutput().get("question"));
        System.out.println("  Entities: " + workflow.getOutput().get("entities"));
        System.out.println("  Total sources: " + workflow.getOutput().get("totalSources"));
        System.out.println("  Answer: " + workflow.getOutput().get("answer"));

        System.out.println("\n--- Knowledge Graph RAG Pattern ---");
        System.out.println("  - Entity extraction: Identify key concepts from the question");
        System.out.println("  - Parallel retrieval: Graph traversal + vector search in FORK/JOIN");
        System.out.println("  - Context merging: Combine structured facts with unstructured docs");
        System.out.println("  - Generation: Produce answer enriched by both sources");

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
