package customerchurn.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccn_deliver";
    }

    @Override
    public TaskResult execute(Task task) {

        String customerId = (String) task.getInputData().get("customerId");
        System.out.printf("  [deliver] Offer delivered to customer %s%n", customerId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", true);
        result.getOutputData().put("channels", java.util.List.of("sms", "email"));
        return result;
    }
}
