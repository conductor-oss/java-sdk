package videoprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ThumbnailWorkerTest {

    @BeforeAll
    static void setHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test void generatesThumbnail() {
        ThumbnailWorker w = new ThumbnailWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("videoId", "VID-001", "duration", 180)));
        TaskResult r = w.execute(t);
        assertEquals(60, r.getOutputData().get("capturedAtSecond"));
        assertEquals(1280, r.getOutputData().get("width"));
    }
}
