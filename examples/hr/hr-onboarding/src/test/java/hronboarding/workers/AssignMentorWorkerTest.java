package hronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssignMentorWorkerTest {

    private final AssignMentorWorker worker = new AssignMentorWorker();

    @Test
    void taskDefName() {
        assertEquals("hro_assign_mentor", worker.getTaskDefName());
    }

    @Test
    void assignsMentor() {
        Task task = taskWith(Map.of("employeeId", "EMP-605", "department", "Engineering"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("EMP-150", result.getOutputData().get("mentorId"));
        assertEquals("Sarah Chen", result.getOutputData().get("mentorName"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
