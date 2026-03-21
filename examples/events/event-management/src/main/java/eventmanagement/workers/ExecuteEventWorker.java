package eventmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExecuteEventWorker implements Worker {
    @Override public String getTaskDefName() { return "evt_execute"; }

    @Override
    public TaskResult execute(Task task) {
        int attendees = task.getInputData().get("attendees") instanceof Number n ? n.intValue() : 0;
        int actual = (int) Math.floor(attendees * 0.82);
        System.out.println("  [execute] Event executed: " + actual + " attendees showed up");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("actualAttendees", actual);
        result.getOutputData().put("sessions", 8);
        result.getOutputData().put("speakersPresent", 12);
        return result;
    }
}
