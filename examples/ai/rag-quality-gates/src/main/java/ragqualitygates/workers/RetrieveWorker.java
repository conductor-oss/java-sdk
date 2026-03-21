package ragqualitygates.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that retrieves documents for the given question.
 * Returns 3 fixed documents with id, text, and relevance score.
 * In production this would query a vector database.
 */
public class RetrieveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qg_retrieve";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        System.out.println("  [retrieve] Retrieving documents for: " + question);

        List<Map<String, Object>> documents = List.of(
                Map.of("id", "doc-1", "text", "Conductor orchestrates microservices using workflow definitions.", "score", 0.92),
                Map.of("id", "doc-2", "text", "Workers poll for tasks and execute business logic independently.", "score", 0.78),
                Map.of("id", "doc-3", "text", "Workflow versioning allows safe rollouts of updated definitions.", "score", 0.55)
        );

        System.out.println("  [retrieve] Retrieved " + documents.size() + " documents");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", documents);
        return result;
    }
}
