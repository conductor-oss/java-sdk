package videoprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PublishWorkerTest {
    @Test void publishesVideo() {
        PublishWorker w = new PublishWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("videoId", "VID-001", "title", "Test")));
        TaskResult r = w.execute(t);
        assertEquals("published", r.getOutputData().get("status"));
    }
}
