package commissioninsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PayWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cin_pay";
    }

    @Override
    public TaskResult execute(Task task) {

        String agentId = (String) task.getInputData().get("agentId");
        System.out.printf("  [pay] Agent %s paid%n", agentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("paid", true);
        result.getOutputData().put("paymentId", "PAY-commission-insurance-001");
        return result;
    }
}
