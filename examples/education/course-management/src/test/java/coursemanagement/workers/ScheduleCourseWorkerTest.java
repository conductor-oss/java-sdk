package coursemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleCourseWorkerTest {

    private final ScheduleCourseWorker worker = new ScheduleCourseWorker();

    @Test
    void taskDefName() {
        assertEquals("crs_schedule", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void schedulesSession() {
        Task task = taskWith(Map.of("courseId", "CS-101", "semester", "Fall 2024"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, String> schedule = (Map<String, String>) result.getOutputData().get("schedule");
        assertNotNull(schedule);
        assertNotNull(schedule.get("days"));
        assertNotNull(schedule.get("time"));
        assertNotNull(schedule.get("room"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
