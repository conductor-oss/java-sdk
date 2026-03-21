package interservicecommunication.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ShippingServiceWorker implements Worker {
    @Override public String getTaskDefName() { return "isc_shipping_service"; }
    @Override public TaskResult execute(Task task) {
        String warehouse = (String) task.getInputData().getOrDefault("warehouse", "unknown");
        System.out.println("  [shipping] Creating shipment from " + warehouse);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("trackingId", "TRACK-" + System.currentTimeMillis());
        r.getOutputData().put("eta", "2 days");
        return r;
    }
}
