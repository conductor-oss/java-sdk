package troubleticket.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DiagnoseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tbt_diagnose";
    }

    @Override
    public TaskResult execute(Task task) {

        String ticketId = (String) task.getInputData().get("ticketId");
        System.out.println("  [diagnose] Line fault detected");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("category", "line-fault");
        result.getOutputData().put("rootCause", "damaged-cable");
        return result;
    }
}
