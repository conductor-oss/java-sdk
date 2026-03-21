package coursemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Schedules course sessions for the semester.
 * Input: courseId, semester
 * Output: schedule (days, time, room)
 */
public class ScheduleCourseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crs_schedule";
    }

    @Override
    public TaskResult execute(Task task) {
        String courseId = (String) task.getInputData().get("courseId");
        Map<String, String> schedule = Map.of(
                "days", "Mon/Wed/Fri",
                "time", "10:00-11:00",
                "room", "Hall 204");

        System.out.println("  [schedule] " + courseId + " -> " + schedule.get("days") + " " + schedule.get("time"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("schedule", schedule);
        return result;
    }
}
