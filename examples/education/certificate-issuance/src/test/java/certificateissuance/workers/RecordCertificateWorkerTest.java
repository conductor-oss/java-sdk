package certificateissuance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RecordCertificateWorkerTest {
    private final RecordCertificateWorker worker = new RecordCertificateWorker();

    @Test void taskDefName() { assertEquals("cer_record", worker.getTaskDefName()); }

    @Test void recordsCertificate() {
        Task task = taskWith(Map.of("certificateId", "CERT-001", "studentId", "STU-001", "courseId", "CS-301"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("recorded"));
        assertEquals(true, result.getOutputData().get("blockchain"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
