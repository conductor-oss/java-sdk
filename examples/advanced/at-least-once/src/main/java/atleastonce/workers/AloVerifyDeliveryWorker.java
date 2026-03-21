package atleastonce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AloVerifyDeliveryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "alo_verify_delivery";
    }

    @Override
    public TaskResult execute(Task task) {
        Object acked = task.getInputData().get("acknowledged");
        boolean isAcked = Boolean.TRUE.equals(acked) || "true".equals(String.valueOf(acked));
        System.out.println("  [verify] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deliveryVerified", isAcked);
        result.getOutputData().put("deliveryGuarantee", "at-least-once");
        return result;
    }
}