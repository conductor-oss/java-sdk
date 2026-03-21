package eventttl.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs an expired event that exceeded its TTL.
 * Input: eventId, age, ttl
 * Output: logged (true), reason ("TTL exceeded")
 */
public class LogExpiredWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xl_log_expired";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        Object age = task.getInputData().get("age");
        Object ttl = task.getInputData().get("ttl");

        System.out.println("  [xl_log_expired] Event " + eventId
                + " expired: age=" + age + "s > TTL=" + ttl + "s");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        result.getOutputData().put("reason", "TTL exceeded");
        return result;
    }
}
