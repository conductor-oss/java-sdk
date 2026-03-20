package monitoringai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class DiagnoseWorker implements Worker {
    @Override public String getTaskDefName() { return "mai_diagnose"; }
    @Override public TaskResult execute(Task task) {
        String rootCause = "Database connection pool exhaustion causing latency spikes and high CPU";
        System.out.println("  [diagnose] Root cause: " + rootCause);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("diagnosis", Map.of("rootCause", rootCause, "confidence", 0.87));
        result.getOutputData().put("rootCause", rootCause);
        return result;
    }
}
