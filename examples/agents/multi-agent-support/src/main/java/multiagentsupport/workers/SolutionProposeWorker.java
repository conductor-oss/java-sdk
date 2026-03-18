package multiagentsupport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Proposes a solution based on knowledge base articles and the ticket description.
 * Returns a response with fix steps, referenced articles, and solution type.
 */
public class SolutionProposeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cs_solution_propose";
    }

    @Override
    public TaskResult execute(Task task) {
        String description = (String) task.getInputData().get("description");
        String severity = (String) task.getInputData().get("severity");

        if (description == null) description = "";
        if (severity == null) severity = "medium";

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> kbArticles = (List<Map<String, Object>>) task.getInputData().get("kbArticles");

        System.out.println("  [cs_solution_propose] Proposing solution for severity: " + severity);

        String response = "Based on our analysis, here are the recommended fix steps:\n"
                + "1. Clear the application cache and restart the service.\n"
                + "2. Verify configuration files match the expected schema.\n"
                + "3. Check system resource utilization (CPU, memory, disk).\n"
                + "4. Apply the latest patch from the downloads portal.\n"
                + "If the issue persists, please provide the diagnostic logs for further investigation.";

        List<String> referencedArticles = List.of("KB-1001", "KB-1002", "KB-1003");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", response);
        result.getOutputData().put("referencedArticles", referencedArticles);
        result.getOutputData().put("solutionType", "known_fix");
        return result;
    }
}
