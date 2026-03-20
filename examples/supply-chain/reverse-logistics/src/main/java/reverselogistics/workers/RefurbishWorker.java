package reverselogistics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class RefurbishWorker implements Worker {
    @Override public String getTaskDefName() { return "rvl_refurbish"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [refurbish] " + task.getInputData().get("returnId") + ": cleaning, testing, repackaging");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "refurbished"); r.getOutputData().put("resaleValue", 120.00); return r;
    }
}
