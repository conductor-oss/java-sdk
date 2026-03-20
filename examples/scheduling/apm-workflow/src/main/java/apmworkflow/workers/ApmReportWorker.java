package apmworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class ApmReportWorker implements Worker {
    @Override public String getTaskDefName() { return "apm_report"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [report] Generating APM report");
        r.getOutputData().put("reportGenerated", true);
        return r;
    }
}
