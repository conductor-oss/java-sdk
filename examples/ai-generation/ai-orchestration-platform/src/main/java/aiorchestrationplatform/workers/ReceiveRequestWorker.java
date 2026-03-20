package aiorchestrationplatform.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReceiveRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aop_receive_request";
    }

    @Override
    public TaskResult execute(Task task) {

        String requestType = (String) task.getInputData().get("requestType");
        String priority = (String) task.getInputData().get("priority");
        System.out.printf("  [receive] Request: %s (priority: high)%n", requestType, priority);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", "REQ-ai-orchestration-platform-001");
        return result;
    }
}
