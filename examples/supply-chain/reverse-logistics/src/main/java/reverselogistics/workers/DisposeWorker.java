package reverselogistics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DisposeWorker implements Worker {
    @Override public String getTaskDefName() { return "rvl_dispose"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [dispose] " + task.getInputData().get("returnId") + ": scheduled for safe disposal");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "disposed"); r.getOutputData().put("method", "certified_waste"); return r;
    }
}
