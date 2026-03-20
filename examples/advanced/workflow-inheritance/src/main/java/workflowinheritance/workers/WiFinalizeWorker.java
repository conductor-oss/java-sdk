package workflowinheritance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WiFinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wi_finalize";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [finalize] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalized", true);
        return result;
    }
}