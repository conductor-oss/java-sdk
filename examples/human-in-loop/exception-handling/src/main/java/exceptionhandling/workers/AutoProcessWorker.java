package exceptionhandling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for eh_auto_process — handles low-risk items automatically.
 *
 * Returns processed=true upon completion.
 */
public class AutoProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eh_auto_process";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [eh_auto_process] Auto-processing item...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        return result;
    }
}
