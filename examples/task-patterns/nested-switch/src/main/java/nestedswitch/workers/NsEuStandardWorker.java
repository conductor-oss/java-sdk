package nestedswitch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles EU region, standard (default) tier requests.
 *
 * Output:
 * - handler (String): "ns_eu_standard"
 * - done (boolean): true
 */
public class NsEuStandardWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ns_eu_standard";
    }

    @Override
    public TaskResult execute(Task task) {
        String region = (String) task.getInputData().get("region");
        String tier = (String) task.getInputData().get("tier");

        System.out.println("  [ns_eu_standard] region=" + region + ", tier=" + tier + " -> EU standard handler");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "ns_eu_standard");
        result.getOutputData().put("done", true);
        return result;
    }
}
