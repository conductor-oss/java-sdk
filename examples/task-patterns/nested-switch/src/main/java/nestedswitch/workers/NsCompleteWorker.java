package nestedswitch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Final completion step after all nested switch branches.
 *
 * Output:
 * - handler (String): "ns_complete"
 * - done (boolean): true
 */
public class NsCompleteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ns_complete";
    }

    @Override
    public TaskResult execute(Task task) {
        String region = (String) task.getInputData().get("region");
        String tier = (String) task.getInputData().get("tier");

        System.out.println("  [ns_complete] region=" + region + ", tier=" + tier + " -> completion handler");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "ns_complete");
        result.getOutputData().put("done", true);
        return result;
    }
}
