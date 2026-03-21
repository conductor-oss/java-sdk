package priorauthorization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SubmitRequestWorkerTest {
    private final SubmitRequestWorker w = new SubmitRequestWorker();
    @Test void taskDefName() { assertEquals("pa_submit_request", w.getTaskDefName()); }
    @Test void submits() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("authId", "A1", "patientId", "P1", "procedure", "MRI", "clinicalReason", "pain")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("submitted"));
    }
}
