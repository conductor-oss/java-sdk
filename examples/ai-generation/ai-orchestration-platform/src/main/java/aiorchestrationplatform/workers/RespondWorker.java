package aiorchestrationplatform.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RespondWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aop_respond";
    }

    @Override
    public TaskResult execute(Task task) {

        String requestId = (String) task.getInputData().get("requestId");
        System.out.printf("  [respond] Request %s responded%n", requestId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("responded", true);
        result.getOutputData().put("statusCode", 200);
        return result;
    }
}
