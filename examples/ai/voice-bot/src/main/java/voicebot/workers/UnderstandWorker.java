package voicebot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class UnderstandWorker implements Worker {
    @Override public String getTaskDefName() { return "vb_understand"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [understand] Intent: order_status, Entity: order#1234");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("intent", "order_status");
        result.getOutputData().put("confidence", 0.91);
        result.getOutputData().put("entities", Map.of("orderNumber", "1234"));
        result.getOutputData().put("sentiment", "neutral");
        return result;
    }
}
