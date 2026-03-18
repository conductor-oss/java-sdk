package chainofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalAnswerWorkerTest {

    private final FinalAnswerWorker worker = new FinalAnswerWorker();

    @Test
    void taskDefName() {
        assertEquals("ct_final_answer", worker.getTaskDefName());
    }

    @Test
    void producesAnswer() {
        Task task = taskWith(Map.of(
                "problem", "What is the compound interest on $10,000 at 5% annual rate for 3 years?",
                "verifiedResult", 11576.25,
                "confidence", 1.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("The compound interest on $10,000 at 5% for 3 years yields $11576.25 (confidence: 1.0)",
                result.getOutputData().get("answer"));
    }

    @Test
    void answerContainsResult() {
        Task task = taskWith(Map.of(
                "problem", "test problem",
                "verifiedResult", 11576.25,
                "confidence", 1.0));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("11576.25"));
    }

    @Test
    void answerContainsConfidence() {
        Task task = taskWith(Map.of(
                "problem", "test problem",
                "verifiedResult", 11576.25,
                "confidence", 1.0));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("1.0"));
    }

    @Test
    void handlesNullProblem() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", null);
        input.put("verifiedResult", 11576.25);
        input.put("confidence", 1.0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesNullVerifiedResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", "test problem");
        input.put("verifiedResult", null);
        input.put("confidence", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void outputContainsAnswerKey() {
        Task task = taskWith(Map.of("problem", "test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("answer"));
        assertInstanceOf(String.class, result.getOutputData().get("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
