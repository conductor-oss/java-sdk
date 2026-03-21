package supplychainmgmt.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Ships the batch to the destination.
 * Input: batchId, destination
 * Output: deliveryId, eta
 */
public class DeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "scm_deliver";
    }

    @Override
    public TaskResult execute(Task task) {
        String batchId = (String) task.getInputData().get("batchId");
        String destination = (String) task.getInputData().get("destination");

        System.out.println("  [deliver] Shipping " + batchId + " to " + destination);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deliveryId", "DEL-651-001");
        result.getOutputData().put("eta", "3 days");
        return result;
    }
}
