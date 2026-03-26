package waittimeoutescalation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for wte_process — processes the response received after the WAIT task completes.
 *
 * Returns { result: "processed-{response}" } where response comes from the WAIT task output.
 */
public class WteProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wte_process";
    }

    @Override
    public TaskResult execute(Task task) {
        Object response = task.getInputData().get("response");
        String responseStr = response != null ? response.toString() : "unknown";

        System.out.println("  [wte_process] Processing response: " + responseStr);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "processed-" + responseStr);

        return result;
    }
}
