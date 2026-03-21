package publicrecords.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseWorkerTest {

    @Test
    void testReleaseWorker() {
        ReleaseWorker worker = new ReleaseWorker();
        assertEquals("pbr_release", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("requesterId", "MEDIA-01", "documents", List.of("DOC-A-R")));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("released"));
        assertEquals(3, result.getOutputData().get("count"));
    }
}
