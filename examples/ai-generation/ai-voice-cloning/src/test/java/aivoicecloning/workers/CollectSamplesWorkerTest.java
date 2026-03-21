package aivoicecloning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectSamplesWorkerTest {

    @Test
    void testCollectSamplesWorker() {
        CollectSamplesWorker worker = new CollectSamplesWorker();
        assertEquals("avc_collect_samples", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("speakerId", "SPK-TEST", "language", "en-US"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(25, result.getOutputData().get("sampleCount"));
    }
}
