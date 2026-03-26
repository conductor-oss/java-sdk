package travelanalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "tan_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Analytics dashboard updated with new insights");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reportId", "TAN-RPT-720"); r.getOutputData().put("published", true); return r;
    }
}
