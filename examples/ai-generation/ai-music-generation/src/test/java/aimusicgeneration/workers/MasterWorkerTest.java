package aimusicgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MasterWorkerTest {

    @Test
    void testMasterWorker() {
        MasterWorker worker = new MasterWorker();
        assertEquals("amg_master", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("trackId", "TRK-TEST"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TRK-ai-music-generation-001-M", result.getOutputData().get("masteredTrackId"));
        assertEquals(-14, result.getOutputData().get("loudness"));
    }
}
