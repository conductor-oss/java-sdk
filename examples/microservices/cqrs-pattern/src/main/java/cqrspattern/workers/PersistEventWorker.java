package cqrspattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PersistEventWorker implements Worker {
    @Override public String getTaskDefName() { return "cqrs_persist_event"; }
    @Override public TaskResult execute(Task task) {
        String eventId = "EVT-" + System.currentTimeMillis();
        System.out.println("  [persist] Event stored: " + eventId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("eventId", eventId);
        r.getOutputData().put("persisted", true);
        return r;
    }
}
