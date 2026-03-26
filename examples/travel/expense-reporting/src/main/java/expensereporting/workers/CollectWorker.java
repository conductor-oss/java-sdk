package expensereporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CollectWorker implements Worker {
    @Override public String getTaskDefName() { return "exr_collect"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Processing expense reporting step");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("receipts", Map.of("count",8,"sources","hotel,meals,transport,misc")); return r; } }
