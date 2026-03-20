package workerpools.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WplExecuteTaskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wpl_execute_task";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [execute] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "task_completed_successfully");
        result.getOutputData().put("durationMs", 500);
        return result;
    }
}