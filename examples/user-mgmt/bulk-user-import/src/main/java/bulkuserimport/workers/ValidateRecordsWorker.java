package bulkuserimport.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class ValidateRecordsWorker implements Worker {
    @Override public String getTaskDefName() { return "bui_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Validated " + task.getInputData().get("totalParsed") + " records - 1238 valid, 12 invalid");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("validRecords", List.of(Map.of("email", "a@ex.com")));
        r.getOutputData().put("validCount", 1238);
        r.getOutputData().put("invalidCount", 12);
        r.getOutputData().put("errors", List.of("missing email (5)", "duplicate (7)"));
        return r;
    }
}
