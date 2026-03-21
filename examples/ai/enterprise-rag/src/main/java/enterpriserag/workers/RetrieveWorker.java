package enterpriserag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that retrieves relevant context documents for a question.
 * Returns 4 context documents with id, text, and token counts.
 */
public class RetrieveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "er_retrieve";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");

        System.out.println("  [retrieve] Retrieving context for question=\"" + question + "\"");

        List<Map<String, Object>> context = List.of(
                Map.of("id", "doc-101", "text",
                        "RAG combines retrieval mechanisms with generative models to produce grounded answers.",
                        "tokens", 142),
                Map.of("id", "doc-202", "text",
                        "Enterprise search systems use vector databases for semantic similarity matching.",
                        "tokens", 118),
                Map.of("id", "doc-303", "text",
                        "Token budgets help control costs by limiting context window usage per request.",
                        "tokens", 95),
                Map.of("id", "doc-404", "text",
                        "Caching strategies reduce latency and cost for frequently asked questions.",
                        "tokens", 87)
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("context", context);
        return result;
    }
}
