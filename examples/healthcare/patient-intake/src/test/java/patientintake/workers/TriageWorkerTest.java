package patientintake.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TriageWorkerTest {
    private final TriageWorker worker = new TriageWorker();

    @Test void taskDefName() { assertEquals("pit_triage", worker.getTaskDefName()); }

    @Test void chestPainTriagesToLevel2Emergency() {
        TaskResult result = worker.execute(taskWith(Map.of("chiefComplaint", "severe chest pain")));
        assertEquals(2, result.getOutputData().get("triageLevel"));
        assertEquals("Emergency", result.getOutputData().get("department"));
    }

    @Test void headacheTriagesToFastTrack() {
        TaskResult result = worker.execute(taskWith(Map.of("chiefComplaint", "mild headache")));
        assertEquals(4, result.getOutputData().get("triageLevel"));
        assertEquals("Fast Track", result.getOutputData().get("department"));
    }

    @Test void handlesNullComplaint() {
        Map<String, Object> input = new HashMap<>(); input.put("chiefComplaint", null);
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void includesAssessedAtTimestamp() {
        TaskResult result = worker.execute(taskWith(Map.of("chiefComplaint", "cough")));
        assertNotNull(result.getOutputData().get("assessedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
