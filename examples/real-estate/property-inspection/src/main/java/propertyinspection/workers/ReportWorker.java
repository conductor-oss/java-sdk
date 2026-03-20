package propertyinspection.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "pin_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [pin_report] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportId", "INSP-RPT-687");
        result.getOutputData().put("findings", Map.of("overallCondition","good","issueCount",3));
        result.getOutputData().put("documentation", Map.of("photos",24,"notes",8));
        return result;
    }
}
