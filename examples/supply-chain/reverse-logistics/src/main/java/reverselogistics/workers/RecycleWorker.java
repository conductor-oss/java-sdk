package reverselogistics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class RecycleWorker implements Worker {
    @Override public String getTaskDefName() { return "rvl_recycle"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [recycle] " + task.getInputData().get("returnId") + ": disassembled for parts recycling");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "recycled"); r.getOutputData().put("materialsRecovered", 3); return r;
    }
}
