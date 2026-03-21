package tutoringmatch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmWorkerTest {
    @Test void taskDefName() { assertEquals("tut_confirm", new ConfirmWorker().getTaskDefName()); }
    @Test void confirmsSession() {
        Task task = taskWith(Map.of("sessionId", "SES-001", "studentId", "STU-001", "tutorId", "TUT-42"));
        TaskResult result = new ConfirmWorker().execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
