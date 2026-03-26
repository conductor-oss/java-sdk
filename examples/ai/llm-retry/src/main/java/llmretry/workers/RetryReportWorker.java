package llmretry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Summarises the retry outcome. Receives the LLM response and the
 * number of attempts it took to succeed.
 */
public class RetryReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "retry_report";
    }

    @Override
    public TaskResult execute(Task task) {
        Object attempts = task.getInputData().get("attempts");
        System.out.println("  [report] Succeeded after " + attempts + " attempts");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", "Retry succeeded");
        return result;
    }
}
