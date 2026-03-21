package backpressure.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BkpHandleOkWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bkp_handle_ok";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [ok] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("action", "proceed_normally");
        result.getOutputData().put("throttled", false);
        return result;
    }
}