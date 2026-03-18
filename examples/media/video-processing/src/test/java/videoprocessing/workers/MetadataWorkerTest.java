package videoprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MetadataWorkerTest {
    @Test void extractsMetadata() {
        MetadataWorker w = new MetadataWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("videoId", "VID-001", "title", "Test Video", "duration", 180)));
        TaskResult r = w.execute(t);
        assertNotNull(r.getOutputData().get("metadata"));
    }
}
