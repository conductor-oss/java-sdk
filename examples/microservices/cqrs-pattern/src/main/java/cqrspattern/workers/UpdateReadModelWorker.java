package cqrspattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class UpdateReadModelWorker implements Worker {
    @Override public String getTaskDefName() { return "cqrs_update_read_model"; }
    @Override public TaskResult execute(Task task) {
        String aggregateId = (String) task.getInputData().getOrDefault("aggregateId", "unknown");
        System.out.println("  [read-model] Projection updated for " + aggregateId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("updated", true);
        return r;
    }
}
