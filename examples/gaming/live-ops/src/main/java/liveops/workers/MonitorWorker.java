package liveops.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class MonitorWorker implements Worker {
    @Override public String getTaskDefName() { return "lop_monitor"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Monitoring event " + task.getInputData().get("eventId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("participants", 15000); r.addOutputData("engagement", "high"); r.addOutputData("issues", 0);
        return r;
    }
}
