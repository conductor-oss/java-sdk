package inspectionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordPassWorkerTest {

    @Test
    void testRecordPassWorker() {
        RecordPassWorker worker = new RecordPassWorker();
        assertEquals("inw_record_pass", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("propertyId", "PROP-500"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CERT-inspection-workflow-001", result.getOutputData().get("certificate"));
    }
}
