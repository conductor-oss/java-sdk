package troubleticket.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssignWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tbt_assign";
    }

    @Override
    public TaskResult execute(Task task) {

        String ticketId = (String) task.getInputData().get("ticketId");
        System.out.printf("  [assign] Ticket %s assigned to field tech FT-22%n", ticketId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assignee", "FT-22");
        result.getOutputData().put("eta", "2024-03-11T09:00:00Z");
        return result;
    }
}
