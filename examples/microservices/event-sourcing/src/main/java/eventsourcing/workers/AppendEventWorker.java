package eventsourcing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AppendEventWorker implements Worker {
    @Override public String getTaskDefName() { return "es_append_event"; }
    @Override public TaskResult execute(Task task) {
        String eventId = "EVT-" + System.currentTimeMillis();
        System.out.println("  [append] Event " + eventId + " appended (version 5)");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("eventId", eventId);
        r.getOutputData().put("version", 5);
        r.getOutputData().put("appended", true);
        return r;
    }
}
