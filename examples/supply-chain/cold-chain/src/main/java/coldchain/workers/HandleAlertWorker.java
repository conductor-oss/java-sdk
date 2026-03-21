package coldchain.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class HandleAlertWorker implements Worker {
    @Override public String getTaskDefName() { return "cch_handle_alert"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [alert] " + task.getInputData().get("shipmentId") + ": temperature " + task.getInputData().get("currentTemp") + " C — ALERT sent");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "alert_sent"); r.getOutputData().put("notified", List.of("logistics_mgr","quality_team")); return r;
    }
}
