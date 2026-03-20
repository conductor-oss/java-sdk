package compliancevendor.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CertifyWorkerTest {
    @Test void taskDefName() { assertEquals("vcm_certify", new CertifyWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("vendorId","VND-001","complianceStandard","ISO-27001","assessmentResult","satisfactory","auditPassed",true,"certificationId","CERT-001")));
        TaskResult r = new CertifyWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
