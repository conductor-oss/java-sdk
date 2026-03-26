package benefitdetermination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculateWorkerTest {

    @Test
    void testCalculateEligible() {
        CalculateWorker worker = new CalculateWorker();
        assertEquals("bnd_calculate", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("eligibility", "eligible", "income", 50000));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(850, result.getOutputData().get("benefitAmount"));
    }
}
