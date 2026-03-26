package reverselogistics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class InspectWorker implements Worker {
    @Override public String getTaskDefName() { return "rvl_inspect"; }
    @Override public TaskResult execute(Task task) {
        String condition = "refurbish";
        System.out.println("  [inspect] " + task.getInputData().get("returnId") + ": condition -> " + condition);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("condition", condition); r.getOutputData().put("damageLevel", "minor"); return r;
    }
}
