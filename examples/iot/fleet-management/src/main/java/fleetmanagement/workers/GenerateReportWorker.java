package fleetmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateReportWorker implements Worker {
    @Override public String getTaskDefName() { return "flt_generate_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Processing " + task.getInputData().getOrDefault("onTimeDelivery", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("onTimeDelivery", true);
        r.getOutputData().put("costEstimate", 45.80);
        r.getOutputData().put("reportId", "RPT-535-001");
        return r;
    }
}
