package coursemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssignInstructorWorkerTest {

    private final AssignInstructorWorker worker = new AssignInstructorWorker();

    @Test
    void taskDefName() {
        assertEquals("crs_assign_instructor", worker.getTaskDefName());
    }

    @Test
    void assignsInstructor() {
        Task task = taskWith(Map.of("courseId", "CS-101", "department", "Computer Science"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("instructor"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
