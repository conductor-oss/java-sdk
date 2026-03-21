package tutoringmatch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ScheduleWorker implements Worker {
    @Override public String getTaskDefName() { return "tut_schedule"; }
    @Override public TaskResult execute(Task task) {
        String sessionTime = (String) task.getInputData().getOrDefault("preferredTime", "3:00 PM");
        System.out.println("  [schedule] Session scheduled: " + task.getInputData().get("studentId") + " with " + task.getInputData().get("tutorId") + " at " + sessionTime);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sessionId", "SES-679-001");
        result.getOutputData().put("sessionTime", sessionTime);
        result.getOutputData().put("location", "Room 101");
        return result;
    }
}
