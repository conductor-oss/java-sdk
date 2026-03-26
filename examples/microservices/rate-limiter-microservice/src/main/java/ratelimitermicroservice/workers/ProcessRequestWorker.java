package ratelimitermicroservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes the request when quota is available.
 * Input: request
 * Output: response
 */
public class ProcessRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rl_process_request";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [rl_process_request] Request processed successfully");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", Map.of("status", "ok"));
        return result;
    }
}
