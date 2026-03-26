package shippingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfirmDeliveryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "shp_confirm";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [confirm] Order " + task.getInputData().get("orderId")
                + ": delivery confirmed (tracking: " + task.getInputData().get("trackingNumber") + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("confirmed", true);
        output.put("notificationSent", true);
        result.setOutputData(output);
        return result;
    }
}
