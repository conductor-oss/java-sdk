package livestreaming.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DistributeStreamWorkerTest {

    private final DistributeStreamWorker worker = new DistributeStreamWorker();

    @Test void taskDefName() { assertEquals("lsm_distribute_stream", worker.getTaskDefName()); }

    @Test void distributesStream() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-1","encodedUrl","https://test","adaptiveBitrates",List.of("1080p","720p"))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void returnsPlaybackUrl() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-2","adaptiveBitrates",List.of("1080p"))));
        assertTrue(r.getOutputData().get("playbackUrl").toString().contains("live.example.com"));
    }

    @Test void returnsCdnNodes() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-3","adaptiveBitrates",List.of())));
        assertEquals(12, r.getOutputData().get("cdnNodes"));
    }

    @Test void returnsViewerCount() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-4","adaptiveBitrates",List.of("720p"))));
        assertEquals(2450, r.getOutputData().get("viewerCount"));
    }

    @Test void returnsRegions() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-5","adaptiveBitrates",List.of("1080p"))));
        @SuppressWarnings("unchecked")
        List<String> regions = (List<String>) r.getOutputData().get("regions");
        assertEquals(4, regions.size());
    }

    @Test void handlesNullBitrates() {
        Map<String,Object> in = new HashMap<>(); in.put("streamId","S-6"); in.put("adaptiveBitrates",null);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesMissingInputs() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("playbackUrl"));
    }

    private Task taskWith(Map<String,Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
