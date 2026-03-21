package livestreaming.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetupStreamWorkerTest {

    private final SetupStreamWorker worker = new SetupStreamWorker();

    @Test void taskDefName() { assertEquals("lsm_setup_stream", worker.getTaskDefName()); }

    @Test void setsUpStream() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-1","channelId","CH-1","title","Test","resolution","1080p")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("ingestUrl"));
    }

    @Test void returnsStreamKey() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-2","title","Live","resolution","720p")));
        assertEquals("sk-522-abc123", r.getOutputData().get("streamKey"));
    }

    @Test void returnsIngestUrl() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-3","title","Demo","resolution","1080p")));
        assertTrue(r.getOutputData().get("ingestUrl").toString().startsWith("rtmp://"));
    }

    @Test void returnsStartedAt() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-4","title","Event","resolution","4K")));
        assertNotNull(r.getOutputData().get("startedAt"));
    }

    @Test void handlesNullTitle() {
        Map<String,Object> in = new HashMap<>(); in.put("streamId","S-5"); in.put("title",null); in.put("resolution","720p");
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesNullResolution() {
        Map<String,Object> in = new HashMap<>(); in.put("streamId","S-6"); in.put("title","X"); in.put("resolution",null);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesMissingInputs() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("ingestUrl"));
    }

    private Task taskWith(Map<String,Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
