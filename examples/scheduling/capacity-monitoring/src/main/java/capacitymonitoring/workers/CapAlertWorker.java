package capacitymonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CapAlertWorker implements Worker {
    @Override public String getTaskDefName() { return "cap_alert"; }
    @Override public TaskResult execute(Task task) {
        int diskDays = 100; try { diskDays = Integer.parseInt(String.valueOf(task.getInputData().get("daysUntilDiskFull"))); } catch (Exception ignored) {}
        boolean shouldAlert = diskDays < 30;
        System.out.println("  [alert] Disk full in " + diskDays + " days - alert: " + shouldAlert);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("alertSent", shouldAlert);
        r.getOutputData().put("severity", shouldAlert ? "warning" : "info");
        return r;
    }
}
