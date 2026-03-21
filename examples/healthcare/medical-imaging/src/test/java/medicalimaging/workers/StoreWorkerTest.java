package medicalimaging.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StoreWorkerTest {
    private final StoreWorker w = new StoreWorker();
    @Test void taskDefName() { assertEquals("img_store", w.getTaskDefName()); }
    @Test void archives() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("studyId", "S1", "imageId", "IMG-1", "reportId", "RPT-1")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("archived"));
    }
}
