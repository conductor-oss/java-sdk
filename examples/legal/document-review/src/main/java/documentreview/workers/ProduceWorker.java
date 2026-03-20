package documentreview.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ProduceWorker implements Worker {
    @Override public String getTaskDefName() { return "drv_produce"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [drv_produce] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("producedCount", "285");
        result.getOutputData().put("privilegedCount", 25);
        result.getOutputData().put("documents", Map.of("total",1500));
        result.getOutputData().put("classified", Map.of("relevant",420));
        result.getOutputData().put("reviewed", Map.of("responsive",310));
        return result;
    }
}
