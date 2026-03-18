package videoprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UploadWorkerTest {
    @Test void uploadsVideo() {
        UploadWorker w = new UploadWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("videoId", "VID-001", "sourceUrl", "https://example.com/video.mp4")));
        TaskResult r = w.execute(t);
        assertNotNull(r.getOutputData().get("storagePath"));
        assertTrue(((Number) r.getOutputData().get("duration")).intValue() > 0);
    }
}
