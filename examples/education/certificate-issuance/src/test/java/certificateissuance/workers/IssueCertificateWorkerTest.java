package certificateissuance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class IssueCertificateWorkerTest {
    private final IssueCertificateWorker worker = new IssueCertificateWorker();

    @Test void taskDefName() { assertEquals("cer_issue", worker.getTaskDefName()); }

    @Test void issuesCertificate() {
        Task task = taskWith(Map.of("certificateId", "CERT-001", "studentId", "STU-001"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("issued"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
