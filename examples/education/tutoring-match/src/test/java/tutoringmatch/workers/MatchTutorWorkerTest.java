package tutoringmatch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MatchTutorWorkerTest {
    @Test void taskDefName() { assertEquals("tut_match_tutor", new MatchTutorWorker().getTaskDefName()); }
    @Test void matchesTutor() {
        Task task = taskWith(Map.of("subject", "Calculus II", "preferredTime", "3 PM"));
        TaskResult result = new MatchTutorWorker().execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("tutorId"));
        assertNotNull(result.getOutputData().get("tutorName"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
