package iotsecurity.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class PatchWorker implements Worker {
    @Override public String getTaskDefName() { return "ios_patch"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [patch] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("patchedDevices", "affected");
        r.getOutputData().put("patchCount", "affected.length");
        return r;
    }
}
