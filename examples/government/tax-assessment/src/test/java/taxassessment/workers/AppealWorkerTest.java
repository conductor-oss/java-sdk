package taxassessment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AppealWorkerTest {

    @Test
    void testAppealWorker() {
        AppealWorker worker = new AppealWorker();
        assertEquals("txa_appeal", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("propertyId", "PROP-901", "taxAmount", 5400.0));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("2024-06-30", result.getOutputData().get("appealDeadline"));
        assertEquals(true, result.getOutputData().get("appealOpen"));
    }
}
