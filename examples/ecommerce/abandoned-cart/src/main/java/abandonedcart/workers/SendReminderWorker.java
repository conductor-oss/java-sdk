package abandonedcart.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;

public class SendReminderWorker implements Worker {
    @Override public String getTaskDefName() { return "abc_send_reminder"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [remind] Sending reminder to " + task.getInputData().get("customerId") + " for cart " + task.getInputData().get("cartId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("reminderSent", true); o.put("channel", "email"); o.put("sentAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
