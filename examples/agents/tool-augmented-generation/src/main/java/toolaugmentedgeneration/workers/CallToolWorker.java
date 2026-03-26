package toolaugmentedgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Invokes the external tool identified by the gap detector and returns
 * the factual result along with its source.
 */
public class CallToolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tg_call_tool";
    }

    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown";
        }

        String toolQuery = (String) task.getInputData().get("toolQuery");
        if (toolQuery == null || toolQuery.isBlank()) {
            toolQuery = "";
        }

        System.out.println("  [tg_call_tool] Calling tool '" + toolName + "' with query: " + toolQuery);

        String toolResult = "Node.js v22.x (Jod) is the current LTS version as of 2025";
        String source = "nodejs.org";
        boolean cached = false;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("toolResult", toolResult);
        result.getOutputData().put("source", source);
        result.getOutputData().put("cached", cached);
        return result;
    }
}
