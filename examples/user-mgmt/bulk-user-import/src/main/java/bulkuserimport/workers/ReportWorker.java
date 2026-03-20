package bulkuserimport.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "bui_report"; }
    @Override public TaskResult execute(Task task) {
        String url = "https://reports.example.com/import-" + System.currentTimeMillis() + ".csv";
        System.out.println("  [report] Import report generated -> " + url);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reportUrl", url);
        r.getOutputData().put("summary", Map.of("parsed", task.getInputData().get("parsed"), "valid", task.getInputData().get("valid"), "inserted", task.getInputData().get("inserted")));
        return r;
    }
}
