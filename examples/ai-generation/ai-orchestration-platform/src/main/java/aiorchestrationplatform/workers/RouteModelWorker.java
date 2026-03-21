package aiorchestrationplatform.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RouteModelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aop_route_model";
    }

    @Override
    public TaskResult execute(Task task) {

        String requestType = (String) task.getInputData().get("requestType");
        String requestId = (String) task.getInputData().get("requestId");
        System.out.printf("  [route] %s -> text-model-v3%n", requestType, requestId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("selectedModel", "text-model-v3");
        result.getOutputData().put("modelEndpoint", "http://models/text-model-v3/predict");
        result.getOutputData().put("loadBalanced", true);
        return result;
    }
}
