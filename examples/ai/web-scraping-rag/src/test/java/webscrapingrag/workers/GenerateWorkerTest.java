package webscrapingrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("wsrag_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswer() {
        List<Map<String, Object>> context = new ArrayList<>();
        context.add(new HashMap<>(Map.of(
                "text", "Orkes Conductor is a platform for orchestrating microservices.",
                "score", 0.94
        )));
        context.add(new HashMap<>(Map.of(
                "text", "Workers are the building blocks of Conductor workflows.",
                "score", 0.89
        )));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How do Conductor workers function?",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("2 scraped sources"));
    }

    @Test
    void answerMentionsConductor() {
        List<Map<String, Object>> context = new ArrayList<>();
        context.add(new HashMap<>(Map.of("text", "Some text", "score", 0.9)));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Conductor"));
    }

    @Test
    void answerReflectsContextSize() {
        List<Map<String, Object>> context = new ArrayList<>();
        context.add(new HashMap<>(Map.of("text", "A", "score", 0.9)));
        context.add(new HashMap<>(Map.of("text", "B", "score", 0.8)));
        context.add(new HashMap<>(Map.of("text", "C", "score", 0.7)));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Question",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("3 scraped sources"));
    }

    @Test
    void outputContainsAnswerField() {
        List<Map<String, Object>> context = new ArrayList<>();
        context.add(new HashMap<>(Map.of("text", "Data", "score", 0.5)));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Q",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
