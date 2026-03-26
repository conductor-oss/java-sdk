package billingtelecom.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectPaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "btl_collect_payment";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [collect] Payment collected");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("paymentStatus", "paid");
        result.getOutputData().put("paidAt", "2024-03-28");
        return result;
    }
}
