package interservicecommunication.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OrderServiceWorker implements Worker {
    @Override public String getTaskDefName() { return "isc_order_service"; }
    @Override public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().getOrDefault("orderId", "unknown");
        System.out.println("  [order] Processing order " + orderId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("orderRef", "REF-" + System.currentTimeMillis());
        r.getOutputData().put("validated", true);
        return r;
    }
}
