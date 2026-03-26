package webscrapingrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that queries the vector store with a question.
 * Returns relevant context chunks with similarity scores.
 */
public class QueryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wsrag_query";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        System.out.println("  [query] Searching for: \"" + question + "\"");

        List<Map<String, Object>> context = List.of(
                Map.of(
                        "text", "Orkes Conductor is a platform for orchestrating microservices, AI pipelines, and business workflows.",
                        "score", 0.94
                ),
                Map.of(
                        "text", "Workers are the building blocks of Conductor workflows. Each worker handles a specific task type.",
                        "score", 0.89
                )
        );

        System.out.println("  [query] Found " + context.size() + " relevant chunks");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("context", context);
        return result;
    }
}
