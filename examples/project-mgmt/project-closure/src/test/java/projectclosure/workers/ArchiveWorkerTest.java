package projectclosure.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ArchiveWorkerTest {
    private final ArchiveWorker worker = new ArchiveWorker();

    @Test void taskDefName() { assertEquals("pcl_archive", worker.getTaskDefName()); }

    @Test void archivesProject() {
        Task task = taskWith(Map.of("projectId", "PRJ-909", "signOff", "approved"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ARC-909", result.getOutputData().get("archiveId"));
        assertEquals(true, result.getOutputData().get("archived"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
