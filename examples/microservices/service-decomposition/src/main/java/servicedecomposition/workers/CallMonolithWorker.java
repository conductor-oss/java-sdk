package servicedecomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Calls the legacy monolith to process the request.
 * Input: request
 * Output: source, result
 */
public class CallMonolithWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_call_monolith";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sd_call_monolith] Processing request in legacy system");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("source", "monolith");
        result.getOutputData().put("result", "processed");
        return result;
    }
}
