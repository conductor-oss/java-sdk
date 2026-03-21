package compliancereview.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class RemediateWorker implements Worker {
    @Override public String getTaskDefName() { return "cmr_remediate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cmr_remediate] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("planId", "REM-695");
        result.getOutputData().put("requirements", Map.of("controls",45));
        result.getOutputData().put("assessment", Map.of("score",84,"met",38,"notMet",7));
        result.getOutputData().put("gaps", java.util.List.of(Map.of("control","encryption","severity","critical")));
        return result;
    }
}
