package publicrecords.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pbr_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String requesterId = (String) task.getInputData().get("requesterId");
        System.out.printf("  [request] FOIA request from %s%n", requesterId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", "FOIA-public-records-001");
        return result;
    }
}
