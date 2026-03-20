package servicedecomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Calls the new microservice to process the request.
 * Input: request
 * Output: source, result
 */
public class CallMicroserviceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_call_microservice";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sd_call_microservice] Processing request in new service");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("source", "microservice");
        result.getOutputData().put("result", "processed");
        return result;
    }
}
