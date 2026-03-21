package outboxpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PublishEventWorker implements Worker {
    @Override public String getTaskDefName() { return "ob_publish_event"; }
    @Override public TaskResult execute(Task task) {
        String dest = (String) task.getInputData().getOrDefault("destination", "unknown");
        System.out.println("  [publish] Published to " + dest);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("published", true);
        r.getOutputData().put("messageId", "MSG-" + System.currentTimeMillis());
        return r;
    }
}
