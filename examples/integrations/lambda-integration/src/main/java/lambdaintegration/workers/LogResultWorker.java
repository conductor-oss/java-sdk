package lambdaintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs a Lambda execution result.
 * Input: functionName, executionResult, duration
 * Output: logged, logGroup
 */
public class LogResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lam_log_result";
    }

    @Override
    public TaskResult execute(Task task) {
        String functionName = (String) task.getInputData().get("functionName");
        Object duration = task.getInputData().get("duration");
        Object executionResult = task.getInputData().get("executionResult");
        System.out.println("  [log] Function: " + functionName + ", Duration: " + duration + "ms, Result: " + executionResult);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        result.getOutputData().put("logGroup", "/aws/lambda/" + functionName);
        return result;
    }
}
