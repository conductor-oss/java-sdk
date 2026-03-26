package backendforfrontend.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TransformMobileWorker implements Worker {
    @Override public String getTaskDefName() { return "bff_transform_mobile"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mobile] Compact response for small screens");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("format", "mobile");
        r.getOutputData().put("fields", "compact");
        r.getOutputData().put("summary", true);
        return r;
    }
}
