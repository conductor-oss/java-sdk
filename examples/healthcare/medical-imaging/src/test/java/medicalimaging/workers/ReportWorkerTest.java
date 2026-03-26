package medicalimaging.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReportWorkerTest {
    private final ReportWorker w = new ReportWorker();
    @Test void taskDefName() { assertEquals("img_report", w.getTaskDefName()); }
    @Test void generatesReport() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("studyId", "S1", "patientId", "P1", "findings", Map.of("severity", "low"))));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("reportId"));
    }
}
