package nestedswitch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles requests from regions other than US or EU (default case).
 *
 * Output:
 * - handler (String): "ns_other_region"
 * - done (boolean): true
 */
public class NsOtherRegionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ns_other_region";
    }

    @Override
    public TaskResult execute(Task task) {
        String region = (String) task.getInputData().get("region");
        String tier = (String) task.getInputData().get("tier");

        System.out.println("  [ns_other_region] region=" + region + ", tier=" + tier + " -> other region handler");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "ns_other_region");
        result.getOutputData().put("done", true);
        return result;
    }
}
