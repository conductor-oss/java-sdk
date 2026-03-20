package selfhealing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiagnoseWorkerTest {

    @Test
    void taskDefName() {
        DiagnoseWorker worker = new DiagnoseWorker();
        assertEquals("sh_diagnose", worker.getTaskDefName());
    }

    @Test
    void returnsDiagnosisAndAction() {
        DiagnoseWorker worker = new DiagnoseWorker();
        Task task = taskWith(Map.of(
                "service", "broken-service",
                "symptoms", "connection_timeouts"
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("connection_pool_exhausted", result.getOutputData().get("diagnosis"));
        assertEquals("restart_connection_pool", result.getOutputData().get("action"));
    }

    @Test
    void handlesMissingInputs() {
        DiagnoseWorker worker = new DiagnoseWorker();
        Task task = taskWith(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("connection_pool_exhausted", result.getOutputData().get("diagnosis"));
        assertEquals("restart_connection_pool", result.getOutputData().get("action"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
