package ragcitation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that retrieves relevant documents for a given question.
 * Returns 4 fixed documents with id, title, page, text, and relevance score.
 * In production this would query a vector database or search index.
 */
public class RetrieveDocsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_retrieve_docs";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [retrieve_docs] Retrieving documents for question");

        List<Map<String, Object>> documents = List.of(
                Map.of("id", "doc-1",
                        "title", "Conductor Architecture Overview",
                        "page", 12,
                        "text", "Conductor uses a task-based workflow model where each step is executed by a worker polling for tasks.",
                        "relevance", 0.95),
                Map.of("id", "doc-2",
                        "title", "Worker Implementation Guide",
                        "page", 34,
                        "text", "Workers can be implemented in any language including Java, Python, Go, and TypeScript using the Conductor SDK.",
                        "relevance", 0.91),
                Map.of("id", "doc-3",
                        "title", "Workflow Versioning Best Practices",
                        "page", 7,
                        "text", "Conductor supports running multiple versions of a workflow simultaneously, enabling safe rollouts and A/B testing.",
                        "relevance", 0.87),
                Map.of("id", "doc-4",
                        "title", "Task Queue Management",
                        "page", 22,
                        "text", "Task queues in Conductor provide automatic load balancing and retry mechanisms for distributed worker pools.",
                        "relevance", 0.83)
        );

        System.out.println("  [retrieve_docs] Found " + documents.size() + " documents");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", documents);
        return result;
    }
}
