package warehousemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ShipWorker implements Worker {
    @Override public String getTaskDefName() { return "wm_ship"; }

    @Override public TaskResult execute(Task task) {
        String packageId = (String) task.getInputData().get("packageId");
        String method = (String) task.getInputData().get("shippingMethod");
        System.out.println("  [ship] " + packageId + " shipped via " + method);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("trackingNumber", "TRK-657-XYZ");
        r.getOutputData().put("carrier", method);
        return r;
    }
}
