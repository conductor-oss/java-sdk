package coldchain.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
public class MonitorTempWorker implements Worker {
    @Override public String getTaskDefName() { return "cch_monitor_temp"; }
    @Override public TaskResult execute(Task task) {
        double currentTemp = 3.2;
        System.out.println("  [monitor] " + task.getInputData().get("shipmentId") + " (" + task.getInputData().get("product") + "): " + currentTemp + " C");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("currentTemp", currentTemp); r.getOutputData().put("humidity", 85); r.getOutputData().put("timestamp", Instant.now().toString()); return r;
    }
}
