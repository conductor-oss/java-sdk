package certificateissuance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GenerateCertificateWorkerTest {
    private final GenerateCertificateWorker worker = new GenerateCertificateWorker();

    @Test void taskDefName() { assertEquals("cer_generate", worker.getTaskDefName()); }

    @Test void generatesCertificate() {
        Task task = taskWith(Map.of("studentId", "STU-001", "courseName", "ML", "completionDate", "2024-05-15"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("certificateId"));
        assertEquals("PDF", result.getOutputData().get("format"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
