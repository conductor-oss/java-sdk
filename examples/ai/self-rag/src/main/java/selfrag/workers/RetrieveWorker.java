package selfrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Retrieves documents relevant to a question.
 * Returns 4 docs: 3 relevant (score >= 0.5) and 1 irrelevant (0.22).
 */
public class RetrieveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sr_retrieve";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        System.out.println("  [retrieve] Searching for: \"" + question + "\"");

        List<Map<String, Object>> documents = List.of(
                Map.of("id", "d1", "text", "Conductor tasks can be SIMPLE (worker) or SYSTEM (built-in).", "score", 0.91),
                Map.of("id", "d2", "text", "FORK_JOIN runs multiple task branches in parallel.", "score", 0.85),
                Map.of("id", "d3", "text", "DO_WHILE loops tasks until a condition is met.", "score", 0.78),
                Map.of("id", "d4", "text", "Unrelated content about pizza recipes.", "score", 0.22)
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", documents);
        return result;
    }
}
