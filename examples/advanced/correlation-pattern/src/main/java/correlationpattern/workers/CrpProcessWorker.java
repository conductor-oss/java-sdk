package correlationpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CrpProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crp_process";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [process] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedCount", 2);
        result.getOutputData().put("outcomes", java.util.List.of(java.util.Map.of("correlationId","TXN-001","status","processed")));
        return result;
    }
}