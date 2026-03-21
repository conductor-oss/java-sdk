package iotsecurity.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class VerifyWorker implements Worker {
    @Override public String getTaskDefName() { return "ios_verify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("allClear", true);
        r.getOutputData().put("verifiedCount", "devices.length");
        return r;
    }
}
