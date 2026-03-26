package benefitdetermination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyEligibilityWorkerTest {

    @Test
    void testEligible() {
        VerifyEligibilityWorker worker = new VerifyEligibilityWorker();
        assertEquals("bnd_verify_eligibility", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("applicantId", "CIT-529", "income", 49999));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("eligible", result.getOutputData().get("eligibility"));
    }

    @Test
    void testIneligible() {
        VerifyEligibilityWorker worker = new VerifyEligibilityWorker();

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("applicantId", "CIT-522", "income", 60000));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ineligible", result.getOutputData().get("eligibility"));
    }
}
