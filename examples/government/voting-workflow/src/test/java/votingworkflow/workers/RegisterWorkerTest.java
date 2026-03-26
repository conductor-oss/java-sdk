package votingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RegisterWorkerTest {

    @Test
    void testRegisterWorker() {
        RegisterWorker worker = new RegisterWorker();
        assertEquals("vtw_register", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("voterId", "VTR-300", "precinct", "PCT-12"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("registered"));
    }
}
