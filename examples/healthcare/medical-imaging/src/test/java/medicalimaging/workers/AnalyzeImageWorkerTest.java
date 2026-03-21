package medicalimaging.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnalyzeImageWorkerTest {
    private final AnalyzeImageWorker w = new AnalyzeImageWorker();
    @Test void taskDefName() { assertEquals("img_analyze", w.getTaskDefName()); }
    @Test void analyzes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("studyId", "S1", "processedImageId", "IMG-P1", "bodyPart", "chest")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("findings"));
    }
}
