package workflowprofiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WfpExecuteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfp_execute";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [execute] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("executionResults", java.util.List.of(java.util.Map.of("iteration", 1, "totalMs", 2050)));
        return result;
    }
}