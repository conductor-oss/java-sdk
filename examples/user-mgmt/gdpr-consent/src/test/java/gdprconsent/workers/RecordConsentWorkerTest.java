package gdprconsent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordConsentWorkerTest {

    private final RecordConsentWorker worker = new RecordConsentWorker();

    @Test
    void taskDefName() {
        assertEquals("gdc_record_consent", worker.getTaskDefName());
    }

    @Test
    void recordsConsent() {
        Task task = taskWith(Map.of("userId", "USR-123", "consents", Map.of("analytics", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
    }

    @Test
    void includesConsentRecord() {
        Task task = taskWith(Map.of("userId", "USR-123", "consents", Map.of("cookies", true)));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("consentRecord"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void consentRecordContainsVersion() {
        Task task = taskWith(Map.of("userId", "USR-123", "consents", Map.of()));
        TaskResult result = worker.execute(task);

        Map<String, Object> record = (Map<String, Object>) result.getOutputData().get("consentRecord");
        assertEquals("2.1", record.get("version"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
