package inspectionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleWorkerTest {

    @Test
    void testScheduleWorker() {
        ScheduleWorker worker = new ScheduleWorker();
        assertEquals("inw_schedule", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("propertyId", "PROP-500", "inspectionType", "building-safety"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("2024-03-15", result.getOutputData().get("scheduledDate"));
    }
}
