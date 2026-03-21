package roamingmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BillWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rmg_bill";
    }

    @Override
    public TaskResult execute(Task task) {

        String subscriberId = (String) task.getInputData().get("subscriberId");
        System.out.printf("  [bill] Subscriber %s billed for roaming%n", subscriberId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("billed", true);
        result.getOutputData().put("invoiceId", "INV-roaming-management-001");
        return result;
    }
}
