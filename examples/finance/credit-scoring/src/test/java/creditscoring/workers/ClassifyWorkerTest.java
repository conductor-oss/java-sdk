package creditscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyWorkerTest {

    private final ClassifyWorker worker = new ClassifyWorker();

    @Test
    void taskDefName() {
        assertEquals("csc_classify", worker.getTaskDefName());
    }

    @Test
    void classifiesExceptional() {
        Task task = taskWith(Map.of("applicantId", "APP-001", "score", 820));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Exceptional", result.getOutputData().get("classification"));
        assertEquals("high", result.getOutputData().get("approvalLikelihood"));
    }

    @Test
    void classifiesVeryGood() {
        Task task = taskWith(Map.of("applicantId", "APP-002", "score", 750));
        TaskResult result = worker.execute(task);

        assertEquals("Very Good", result.getOutputData().get("classification"));
        assertEquals("high", result.getOutputData().get("approvalLikelihood"));
    }

    @Test
    void classifiesGood() {
        Task task = taskWith(Map.of("applicantId", "APP-003", "score", 700));
        TaskResult result = worker.execute(task);

        assertEquals("Good", result.getOutputData().get("classification"));
        assertEquals("high", result.getOutputData().get("approvalLikelihood"));
    }

    @Test
    void classifiesFair() {
        Task task = taskWith(Map.of("applicantId", "APP-004", "score", 600));
        TaskResult result = worker.execute(task);

        assertEquals("Fair", result.getOutputData().get("classification"));
        assertEquals("low", result.getOutputData().get("approvalLikelihood"));
    }

    @Test
    void classifiesPoor() {
        Task task = taskWith(Map.of("applicantId", "APP-005", "score", 500));
        TaskResult result = worker.execute(task);

        assertEquals("Poor", result.getOutputData().get("classification"));
        assertEquals("low", result.getOutputData().get("approvalLikelihood"));
    }

    @Test
    void borderlineGoodAt670() {
        Task task = taskWith(Map.of("applicantId", "APP-006", "score", 670));
        TaskResult result = worker.execute(task);

        assertEquals("Good", result.getOutputData().get("classification"));
        assertEquals("high", result.getOutputData().get("approvalLikelihood"));
    }

    @Test
    void handlesZeroScore() {
        Task task = taskWith(Map.of("applicantId", "APP-007", "score", 0));
        TaskResult result = worker.execute(task);

        assertEquals("Poor", result.getOutputData().get("classification"));
        assertEquals("low", result.getOutputData().get("approvalLikelihood"));
    }

    @Test
    void handlesNullScore() {
        Map<String, Object> input = new HashMap<>();
        input.put("applicantId", "APP-008");
        input.put("score", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Poor", result.getOutputData().get("classification"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesStringScore() {
        Task task = taskWith(Map.of("applicantId", "APP-009", "score", "820"));
        TaskResult result = worker.execute(task);

        assertEquals("Exceptional", result.getOutputData().get("classification"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
