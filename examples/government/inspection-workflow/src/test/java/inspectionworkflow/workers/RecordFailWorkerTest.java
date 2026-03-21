package inspectionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordFailWorkerTest {

    @Test
    void testRecordFailWorker() {
        RecordFailWorker worker = new RecordFailWorker();
        assertEquals("inw_record_fail", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("propertyId", "PROP-500", "violations", "code-violations"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reinspectionRequired"));
    }
}
