package uptimemonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class UmReportWorker implements Worker {
    @Override public String getTaskDefName() { return "um_report"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [report] Generating SLA report");
        r.getOutputData().put("reportGenerated", true);
        return r;
    }
}
