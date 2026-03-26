package livestreaming.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EncodeStreamWorkerTest {

    private final EncodeStreamWorker worker = new EncodeStreamWorker();

    @Test void taskDefName() { assertEquals("lsm_encode_stream", worker.getTaskDefName()); }

    @Test void encodesStream() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-1","ingestUrl","rtmp://test","resolution","1080p")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void returnsEncodedUrl() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-2","resolution","1080p")));
        assertTrue(r.getOutputData().get("encodedUrl").toString().contains("m3u8"));
    }

    @Test void returnsAdaptiveBitrates() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-3","resolution","720p")));
        @SuppressWarnings("unchecked")
        List<String> bitrates = (List<String>) r.getOutputData().get("adaptiveBitrates");
        assertEquals(4, bitrates.size());
    }

    @Test void returnsCodec() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-4","resolution","1080p")));
        assertEquals("h264", r.getOutputData().get("codec"));
    }

    @Test void returnsLatency() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-5","resolution","1080p")));
        assertEquals(5000, r.getOutputData().get("latencyMs"));
    }

    @Test void handlesNullResolution() {
        Map<String,Object> in = new HashMap<>(); in.put("streamId","S-6"); in.put("resolution",null);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesMissingInputs() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("encodedUrl"));
    }

    private Task taskWith(Map<String,Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
