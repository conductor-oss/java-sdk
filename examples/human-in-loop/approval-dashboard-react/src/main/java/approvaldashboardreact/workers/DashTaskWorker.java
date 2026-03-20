package approvaldashboardreact.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for dash_task — processes the initial task before the approval wait step.
 *
 * Returns { processed: true } to indicate the task has been processed
 * and is ready for the pending_approval WAIT task.
 */
public class DashTaskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dash_task";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [dash_task] Processing task for workflow: " + task.getWorkflowInstanceId());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);

        return result;
    }
}
