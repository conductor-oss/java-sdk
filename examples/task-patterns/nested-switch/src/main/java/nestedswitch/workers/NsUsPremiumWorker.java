package nestedswitch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles US region, premium tier requests.
 *
 * Output:
 * - handler (String): "ns_us_premium"
 * - done (boolean): true
 */
public class NsUsPremiumWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ns_us_premium";
    }

    @Override
    public TaskResult execute(Task task) {
        String region = (String) task.getInputData().get("region");
        String tier = (String) task.getInputData().get("tier");

        System.out.println("  [ns_us_premium] region=" + region + ", tier=" + tier + " -> US premium handler");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "ns_us_premium");
        result.getOutputData().put("done", true);
        return result;
    }
}
