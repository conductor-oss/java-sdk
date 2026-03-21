package eventmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RegisterAttendeesWorker implements Worker {
    @Override public String getTaskDefName() { return "evt_register"; }

    @Override
    public TaskResult execute(Task task) {
        int capacity = task.getInputData().get("capacity") instanceof Number n ? n.intValue() : 200;
        int registered = Math.min(capacity, 187);
        System.out.println("  [register] " + registered + " attendees registered (capacity: " + capacity + ")");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("registeredCount", registered);
        result.getOutputData().put("waitlist", 23);
        return result;
    }
}
