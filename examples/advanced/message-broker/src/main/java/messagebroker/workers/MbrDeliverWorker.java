package messagebroker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MbrDeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mbr_deliver";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [deliver] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", true);
        result.getOutputData().put("deliveryLatencyMs", 23);
        return result;
    }
}