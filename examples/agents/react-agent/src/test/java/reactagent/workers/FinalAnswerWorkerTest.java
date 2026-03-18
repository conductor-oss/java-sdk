package reactagent.workers;

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
        assertEquals("rx_final_answer", worker.getTaskDefName());
    }

    @Test
    void producesExpectedAnswer() {
        Task task = taskWith(Map.of(
                "question", "What is the current world population?",
                "totalIterations", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertEquals("The world population is approximately 8.1 billion people as of 2024, confirmed by multiple sources.",
                answer);
    }

    @Test
    void returnsConfidenceScore() {
        Task task = taskWith(Map.of(
                "question", "What is the current world population?",
                "totalIterations", 3));
        TaskResult result = worker.execute(task);

        assertEquals(0.95, result.getOutputData().get("confidence"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        input.put("totalIterations", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesMissingTotalIterations() {
        Task task = taskWith(Map.of("question", "What is the current world population?"));
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
        assertNotNull(result.getOutputData().get("confidence"));
    }

    @Test
    void answerContainsPopulationFigure() {
        Task task = taskWith(Map.of("question", "Q", "totalIterations", 3));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("8.1 billion"));
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of("question", "Q", "totalIterations", 3));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("answer"));
        assertTrue(result.getOutputData().containsKey("confidence"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
