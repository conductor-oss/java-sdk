package edgecomputing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class ProcessEdgeWorker implements Worker {
    @Override public String getTaskDefName() { return "edg_process_edge"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [process] Processing " + task.getInputData().getOrDefault("edgeResult", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("edgeResult", Map.of());
        r.getOutputData().put("objectsDetected", 12);
        r.getOutputData().put("classifications", List.of("vehicle"));
        r.getOutputData().put("confidenceAvg", 0.91);
        return r;
    }
}
