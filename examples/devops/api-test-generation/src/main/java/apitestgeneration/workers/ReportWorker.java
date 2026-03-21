package apitestgeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.HashMap;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "atg_report"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> r = (Map<String, Object>) task.getInputData().getOrDefault("results", Map.of());
        System.out.println("  [report] Test report: " + r.getOrDefault("passed", 0) + "/" + r.getOrDefault("total", 0) + " passed");
        Map<String, Object> report = new HashMap<>(r);
        report.put("status", "complete");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", report);
        return result;
    }
}
