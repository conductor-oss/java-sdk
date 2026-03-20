package coursemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PublishCourseWorkerTest {

    private final PublishCourseWorker worker = new PublishCourseWorker();

    @Test
    void taskDefName() {
        assertEquals("crs_publish", worker.getTaskDefName());
    }

    @Test
    void publishesCourse() {
        Task task = taskWith(Map.of("courseId", "CS-101", "schedule", Map.of("days", "Mon"), "instructor", "Dr. Chen"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
        assertEquals(true, result.getOutputData().get("enrollmentOpen"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
