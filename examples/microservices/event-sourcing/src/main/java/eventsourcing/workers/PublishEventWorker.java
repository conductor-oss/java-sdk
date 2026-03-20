package eventsourcing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PublishEventWorker implements Worker {
    @Override public String getTaskDefName() { return "es_publish_event"; }
    @Override public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().getOrDefault("eventId", "unknown");
        System.out.println("  [publish] Published " + eventId + " to subscribers");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("published", true);
        return r;
    }
}
