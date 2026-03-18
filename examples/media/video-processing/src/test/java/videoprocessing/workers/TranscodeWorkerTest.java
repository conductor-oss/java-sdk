package videoprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TranscodeWorkerTest {
    @Test void producesMultipleResolutions() {
        TranscodeWorker w = new TranscodeWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("videoId", "VID-001", "storagePath", "s3://test/video.mp4")));
        TaskResult r = w.execute(t);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resolutions = (List<Map<String, Object>>) r.getOutputData().get("resolutions");
        assertEquals(3, resolutions.size());
        assertNotNull(r.getOutputData().get("method"));
    }
}
