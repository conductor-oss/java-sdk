package medicalimaging.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AcquireWorkerTest {
    private final AcquireWorker w = new AcquireWorker();
    @Test void taskDefName() { assertEquals("img_acquire", w.getTaskDefName()); }
    @Test void acquiresImage() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("studyId", "S1", "patientId", "P1", "modality", "CT", "bodyPart", "chest")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("imageId"));
    }
}
