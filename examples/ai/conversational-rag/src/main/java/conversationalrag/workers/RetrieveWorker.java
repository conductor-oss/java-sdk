package conversationalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that retrieves relevant documents based on the contextual query.
 * Returns 3 fixed documents with text and similarity scores.
 * In production this would query a vector database.
 */
public class RetrieveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crag_retrieve";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [retrieve] Searching with contextual query");

        // Fixed deterministic search results
        List<Map<String, Object>> documents = List.of(
                Map.of("text", "Conductor supports workflow versioning, allowing multiple versions of a workflow to coexist.",
                        "score", 0.93),
                Map.of("text", "Task workers can be written in any language that supports HTTP polling.",
                        "score", 0.88),
                Map.of("text", "Workflow inputs and outputs are passed as JSON, enabling flexible data flow.",
                        "score", 0.84)
        );

        System.out.println("  [retrieve] Found " + documents.size() + " relevant documents");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", documents);
        return result;
    }
}
