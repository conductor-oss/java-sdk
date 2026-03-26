package tutoringmatch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class StudentRequestWorkerTest {
    @Test void taskDefName() { assertEquals("tut_student_request", new StudentRequestWorker().getTaskDefName()); }
    @Test void createsRequest() {
        Task task = taskWith(Map.of("studentId", "STU-001", "subject", "Math"));
        TaskResult result = new StudentRequestWorker().execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("requestId"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
