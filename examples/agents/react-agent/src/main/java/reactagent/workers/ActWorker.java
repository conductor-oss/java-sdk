package reactagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Executes the action decided by the ReasonWorker. If the action is
 * "search", returns a deterministic search result. Otherwise returns
 * a synthesis confirmation.
 */
public class ActWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rx_act";
    }

    @Override
    public TaskResult execute(Task task) {
        String thought = (String) task.getInputData().get("thought");
        if (thought == null) {
            thought = "";
        }

        String action = (String) task.getInputData().get("action");
        if (action == null) {
            action = "";
        }

        System.out.println("  [rx_act] Action: " + action);

        String actionResult;
        String source;

        if ("search".equals(action)) {
            actionResult = "World population is approximately 8.1 billion as of 2024";
            source = "search";
        } else {
            actionResult = "Sufficient evidence collected";
            source = "synthesis";
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", actionResult);
        result.getOutputData().put("source", source);
        return result;
    }
}
