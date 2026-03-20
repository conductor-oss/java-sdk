package troubleticket.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CloseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tbt_close";
    }

    @Override
    public TaskResult execute(Task task) {

        String ticketId = (String) task.getInputData().get("ticketId");
        System.out.printf("  [close] Ticket %s closed%n", ticketId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("closedAt", "2024-03-11T13:00:00Z");
        result.getOutputData().put("satisfactionSurvey", "sent");
        return result;
    }
}
