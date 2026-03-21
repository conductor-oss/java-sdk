package citizenrequest.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SubmitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ctz_submit";
    }

    @Override
    public TaskResult execute(Task task) {
        String citizenId = (String) task.getInputData().get("citizenId");
        System.out.printf("  [submit] Request from citizen %s%n", citizenId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", "REQ-citizen-request-001");
        return result;
    }
}
