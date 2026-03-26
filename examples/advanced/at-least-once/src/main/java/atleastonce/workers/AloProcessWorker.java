package atleastonce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AloProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "alo_process";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [process] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("result", java.util.Map.of("status", "success", "recordsUpdated", 3));
        return result;
    }
}