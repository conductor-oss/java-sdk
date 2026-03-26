package privilegedaccess.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PamApproveWorkerTest {

    private final PamApproveWorker worker = new PamApproveWorker();

    @Test
    void taskDefName() {
        assertEquals("pam_approve", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("approveData", Map.of("requestId", "REQUEST-1391")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsApproveFlag() {
        Task task = taskWith(Map.of("approveData", Map.of("requestId", "REQUEST-1391")));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("approve"));
    }

    @Test
    void outputContainsProcessedFlag() {
        Task task = taskWith(Map.of("approveData", Map.of("requestId", "REQUEST-1391")));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullApproveData() {
        Map<String, Object> input = new HashMap<>();
        input.put("approveData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputIsDeterministic() {
        Task task = taskWith(Map.of("approveData", Map.of("requestId", "REQUEST-1391")));
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);

        assertEquals(r1.getOutputData().get("approve"), r2.getOutputData().get("approve"));
        assertEquals(r1.getOutputData().get("processed"), r2.getOutputData().get("processed"));
    }

    @Test
    void outputHasTwoEntries() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
