package livestreaming.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveStreamWorkerTest {

    private final ArchiveStreamWorker worker = new ArchiveStreamWorker();

    @Test void taskDefName() { assertEquals("lsm_archive_stream", worker.getTaskDefName()); }

    @Test void archivesStream() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-1","title","Test Stream","duration",5500)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void returnsArchiveUrl() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-2","title","Demo","duration",3600)));
        assertTrue(r.getOutputData().get("archiveUrl").toString().contains("vod.example.com"));
    }

    @Test void returnsArchiveSize() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-3","title","Live","duration",1800)));
        assertEquals("4.2 GB", r.getOutputData().get("archiveSize"));
    }

    @Test void returnsThumbnailUrl() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-4","title","Event","duration",5400)));
        assertNotNull(r.getOutputData().get("thumbnailUrl"));
    }

    @Test void returnsArchivedAt() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-5","title","Show","duration",900)));
        assertNotNull(r.getOutputData().get("archivedAt"));
    }

    @Test void handlesNullTitle() {
        Map<String,Object> in = new HashMap<>(); in.put("streamId","S-6"); in.put("title",null); in.put("duration",60);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesMissingInputs() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("archiveUrl"));
    }

    private Task taskWith(Map<String,Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
