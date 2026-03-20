package coursemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateCourseWorkerTest {

    private final CreateCourseWorker worker = new CreateCourseWorker();

    @Test
    void taskDefName() {
        assertEquals("crs_create", worker.getTaskDefName());
    }

    @Test
    void createsCourse() {
        Task task = taskWith(Map.of("courseName", "Data Structures", "department", "CS", "credits", 4));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("courseId"));
    }

    @Test
    void courseIdIsNonEmpty() {
        Task task = taskWith(Map.of("courseName", "Algorithms", "department", "CS", "credits", 3));
        TaskResult result = worker.execute(task);
        String courseId = (String) result.getOutputData().get("courseId");
        assertFalse(courseId.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
