package workflowdebugging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Executes the instrumented workflow and captures an execution ID.
 * Input: instrumentedWorkflow
 * Output: executionId, durationMs, taskCount
 */
public class WfdExecuteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfd_execute";
    }

    @Override
    public TaskResult execute(Task task) {
        String instrumentedWorkflow = (String) task.getInputData().getOrDefault("instrumentedWorkflow", "unknown");

        System.out.println("  [execute] Executing instrumented workflow \"" + instrumentedWorkflow + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("executionId", "exec-" + Long.toHexString(System.nanoTime()));
        result.getOutputData().put("durationMs", 3420);
        result.getOutputData().put("taskCount", 7);
        return result;
    }
}
