package labresults.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;
public class LabNotifyWorker implements Worker {
    @Override public String getTaskDefName() { return "lab_notify"; }
    @Override public TaskResult execute(Task task) {
        boolean critical = "true".equals(String.valueOf(task.getInputData().get("critical")));
        String channel = critical ? "phone+portal" : "portal";
        System.out.println("  [notify] Physician notified via " + channel + " for report " + task.getInputData().get("reportId"));
        Map<String, Object> o = new LinkedHashMap<>(); o.put("notified", true); o.put("channel", channel); o.put("notifiedAt", Instant.now().toString());
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.setOutputData(o); return r;
    }
}
