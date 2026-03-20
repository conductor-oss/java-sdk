package reinsurance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CedeWorkerTest {

    @Test
    void testCedeWorker() {
        CedeWorker worker = new CedeWorker();
        assertEquals("rin_cede", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("treatyId", "TRT-reinsurance-QS", "cessionAmount", "5000000"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CES-reinsurance-001", result.getOutputData().get("cessionId"));
    }
}
