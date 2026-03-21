package certificateissuance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SignCertificateWorkerTest {
    private final SignCertificateWorker worker = new SignCertificateWorker();

    @Test void taskDefName() { assertEquals("cer_sign", worker.getTaskDefName()); }

    @Test void signsCertificate() {
        Task task = taskWith(Map.of("certificateId", "CERT-001"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("signed"));
        assertNotNull(result.getOutputData().get("signedBy"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
