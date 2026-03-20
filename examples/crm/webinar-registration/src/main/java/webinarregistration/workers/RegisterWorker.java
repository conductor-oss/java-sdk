package webinarregistration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RegisterWorker implements Worker {
    @Override public String getTaskDefName() { return "wbr_register"; }

    @Override
    public TaskResult execute(Task task) {
        String regId = "REG-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String name = (String) task.getInputData().get("name");
        System.out.println("  [register] " + name + " registered for webinar " + task.getInputData().get("webinarId") + " -> " + regId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("registrationId", regId);
        result.getOutputData().put("joinLink", "https://webinar.example.com/join/" + regId);
        result.getOutputData().put("calendarAdded", true);
        return result;
    }
}
