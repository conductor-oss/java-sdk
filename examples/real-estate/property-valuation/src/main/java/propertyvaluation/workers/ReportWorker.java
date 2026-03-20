package propertyvaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "pvl_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Valuation report generated");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportId", "VAL-RPT-682");
        result.getOutputData().put("generated", true);
        return result;
    }
}
