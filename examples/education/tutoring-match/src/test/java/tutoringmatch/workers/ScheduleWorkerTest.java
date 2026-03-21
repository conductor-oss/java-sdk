package tutoringmatch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleWorkerTest {
    @Test void taskDefName() { assertEquals("tut_schedule", new ScheduleWorker().getTaskDefName()); }
    @Test void schedulesSession() {
        Task task = taskWith(Map.of("studentId", "STU-001", "tutorId", "TUT-42", "preferredTime", "3 PM"));
        TaskResult result = new ScheduleWorker().execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("sessionId"));
        assertNotNull(result.getOutputData().get("sessionTime"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
