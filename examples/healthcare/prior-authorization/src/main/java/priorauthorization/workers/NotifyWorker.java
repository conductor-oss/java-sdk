package priorauthorization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

public class NotifyWorker implements Worker {

    @Override
    public String getTaskDefName() { return "pa_notify"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [notify] Notification sent for auth " + task.getInputData().get("authId")
                + ": " + task.getInputData().get("decision"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("notified", true);
        output.put("channels", List.of("fax", "portal"));
        output.put("notifiedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
