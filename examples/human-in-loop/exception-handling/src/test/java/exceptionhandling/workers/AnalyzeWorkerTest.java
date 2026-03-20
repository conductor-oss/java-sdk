package exceptionhandling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeWorkerTest {

    @Test
    void taskDefName() {
        AnalyzeWorker worker = new AnalyzeWorker();
        assertEquals("eh_analyze", worker.getTaskDefName());
    }

    @Test
    void lowRiskRoutesToAutoProcess() {
        AnalyzeWorker worker = new AnalyzeWorker();
        Task task = taskWith(Map.of("risk", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("auto_process", result.getOutputData().get("route"));
        assertEquals(3, result.getOutputData().get("risk"));
    }

    @Test
    void highRiskRoutesToHumanReview() {
        AnalyzeWorker worker = new AnalyzeWorker();
        Task task = taskWith(Map.of("risk", 9));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("human_review", result.getOutputData().get("route"));
        assertEquals(9, result.getOutputData().get("risk"));
    }

    @Test
    void riskExactlySevenRoutesToAutoProcess() {
        AnalyzeWorker worker = new AnalyzeWorker();
        Task task = taskWith(Map.of("risk", 7));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("auto_process", result.getOutputData().get("route"));
        assertEquals(7, result.getOutputData().get("risk"));
    }

    @Test
    void riskEightRoutesToHumanReview() {
        AnalyzeWorker worker = new AnalyzeWorker();
        Task task = taskWith(Map.of("risk", 8));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("human_review", result.getOutputData().get("route"));
        assertEquals(8, result.getOutputData().get("risk"));
    }

    @Test
    void missingRiskDefaultsToZeroAndAutoProcess() {
        AnalyzeWorker worker = new AnalyzeWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("auto_process", result.getOutputData().get("route"));
        assertEquals(0, result.getOutputData().get("risk"));
    }

    @Test
    void nonNumericRiskDefaultsToZeroAndAutoProcess() {
        AnalyzeWorker worker = new AnalyzeWorker();
        Task task = taskWith(Map.of("risk", "not-a-number"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("auto_process", result.getOutputData().get("route"));
        assertEquals(0, result.getOutputData().get("risk"));
    }

    @Test
    void riskZeroRoutesToAutoProcess() {
        AnalyzeWorker worker = new AnalyzeWorker();
        Task task = taskWith(Map.of("risk", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("auto_process", result.getOutputData().get("route"));
        assertEquals(0, result.getOutputData().get("risk"));
    }

    @Test
    void riskTenRoutesToHumanReview() {
        AnalyzeWorker worker = new AnalyzeWorker();
        Task task = taskWith(Map.of("risk", 10));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("human_review", result.getOutputData().get("route"));
        assertEquals(10, result.getOutputData().get("risk"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
