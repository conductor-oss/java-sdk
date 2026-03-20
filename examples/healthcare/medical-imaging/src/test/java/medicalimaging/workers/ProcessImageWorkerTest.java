package medicalimaging.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProcessImageWorkerTest {
    private final ProcessImageWorker w = new ProcessImageWorker();
    @Test void taskDefName() { assertEquals("img_process", w.getTaskDefName()); }
    @Test void processesImage() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("studyId", "S1", "imageId", "IMG-1", "modality", "CT")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("processedImageId"));
    }
}
