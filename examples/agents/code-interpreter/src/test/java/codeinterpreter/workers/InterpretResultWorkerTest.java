package codeinterpreter.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InterpretResultWorkerTest {

    private final InterpretResultWorker worker = new InterpretResultWorker();

    @Test
    void taskDefName() {
        assertEquals("ci_interpret_result", worker.getTaskDefName());
    }

    @Test
    void returnsAnswerOnSuccess() {
        Task task = taskWith(Map.of(
                "question", "What is the average sales by region?",
                "executionOutput", Map.of("stdout", "West 45200.50"),
                "executionStatus", "success"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("West"));
        assertTrue(answer.contains("45,200.50"));
    }

    @Test
    void returnsInterpretationWithTopRegion() {
        Task task = taskWith(Map.of(
                "question", "Best region?",
                "executionOutput", Map.of("stdout", "data"),
                "executionStatus", "success"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> interp = (Map<String, Object>) result.getOutputData().get("interpretation");
        assertNotNull(interp);
        assertEquals("West", interp.get("topRegion"));
        assertEquals(45200.50, interp.get("topAvgSales"));
    }

    @Test
    void returnsInterpretationWithBottomRegion() {
        Task task = taskWith(Map.of(
                "question", "Sales analysis",
                "executionOutput", Map.of("stdout", "data"),
                "executionStatus", "success"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> interp = (Map<String, Object>) result.getOutputData().get("interpretation");
        assertEquals("Southwest", interp.get("bottomRegion"));
        assertEquals(31200.80, interp.get("bottomAvgSales"));
    }

    @Test
    void returnsTotalRegionsAndInsight() {
        Task task = taskWith(Map.of(
                "question", "Region data",
                "executionOutput", Map.of("stdout", "output"),
                "executionStatus", "success"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> interp = (Map<String, Object>) result.getOutputData().get("interpretation");
        assertEquals(5, interp.get("totalRegions"));

        String insight = (String) interp.get("insight");
        assertNotNull(insight);
        assertTrue(insight.contains("West"));
    }

    @Test
    void returnsHighConfidenceOnSuccess() {
        Task task = taskWith(Map.of(
                "question", "Sales?",
                "executionOutput", Map.of("stdout", "data"),
                "executionStatus", "success"));
        TaskResult result = worker.execute(task);

        assertEquals(0.96, result.getOutputData().get("confidence"));
    }

    @Test
    void returnsFailureAnswerOnNonSuccessStatus() {
        Task task = taskWith(Map.of(
                "question", "What are sales?",
                "executionStatus", "error"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("did not complete successfully"));

        assertEquals(0.0, result.getOutputData().get("confidence"));
    }

    @Test
    void returnsZeroRegionsOnFailure() {
        Task task = taskWith(Map.of(
                "question", "Test",
                "executionStatus", "failed"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> interp = (Map<String, Object>) result.getOutputData().get("interpretation");
        assertEquals("unknown", interp.get("topRegion"));
        assertEquals(0.0, interp.get("topAvgSales"));
        assertEquals(0, interp.get("totalRegions"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        input.put("executionOutput", Map.of("stdout", "data"));
        input.put("executionStatus", "success");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
