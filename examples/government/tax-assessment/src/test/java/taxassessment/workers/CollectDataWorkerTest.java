package taxassessment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectDataWorkerTest {

    @Test
    void testCollectDataWorker() {
        CollectDataWorker worker = new CollectDataWorker();
        assertEquals("txa_collect_data", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("propertyId", "PROP-901", "taxYear", "2024"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("propertyData"));
    }
}
