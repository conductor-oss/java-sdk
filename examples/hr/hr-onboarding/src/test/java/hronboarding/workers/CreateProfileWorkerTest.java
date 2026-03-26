package hronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateProfileWorkerTest {

    private final CreateProfileWorker worker = new CreateProfileWorker();

    @Test
    void taskDefName() {
        assertEquals("hro_create_profile", worker.getTaskDefName());
    }

    @Test
    void createsProfile() {
        Task task = taskWith(Map.of("employeeName", "John Doe", "department", "Engineering"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("EMP-605", result.getOutputData().get("employeeId"));
        assertNotNull(result.getOutputData().get("email"));
    }

    @Test
    void handlesNullName() {
        Map<String, Object> input = new HashMap<>();
        input.put("employeeName", null);
        input.put("department", "HR");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
