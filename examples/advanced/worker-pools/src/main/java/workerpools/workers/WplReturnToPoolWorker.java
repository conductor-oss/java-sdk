package workerpools.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WplReturnToPoolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wpl_return_to_pool";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [return] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("returned", true);
        result.getOutputData().put("poolAvailability", "8/10");
        return result;
    }
}