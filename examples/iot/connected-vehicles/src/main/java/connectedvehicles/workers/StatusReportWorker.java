package connectedvehicles.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class StatusReportWorker implements Worker {
    @Override public String getTaskDefName() { return "veh_status_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [status] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reportGenerated", true);
        return r;
    }
}
