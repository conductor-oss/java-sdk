package troubleticket.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OpenWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tbt_open";
    }

    @Override
    public TaskResult execute(Task task) {

        String customerId = (String) task.getInputData().get("customerId");
        System.out.printf("  [open] Ticket opened for customer %s%n", customerId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ticketId", "TKT-trouble-ticket-001");
        result.getOutputData().put("priority", "high");
        return result;
    }
}
