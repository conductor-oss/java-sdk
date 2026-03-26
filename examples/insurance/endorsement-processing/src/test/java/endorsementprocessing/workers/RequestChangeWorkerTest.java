package endorsementprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RequestChangeWorkerTest {

    @Test
    void testRequestChangeWorker() {
        RequestChangeWorker worker = new RequestChangeWorker();
        assertEquals("edp_request_change", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("policyId", "POL-704", "changeType", "add-vehicle"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("END-endorsement-processing-001", result.getOutputData().get("endorsementId"));
    }
}
