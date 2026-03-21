package ragfusion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateAnswerWorkerTest {

    private final GenerateAnswerWorker worker = new GenerateAnswerWorker();

    @Test
    void taskDefName() {
        assertEquals("rf_generate_answer", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerWithContext() {
        List<Map<String, Object>> fusedContext = List.of(
                new HashMap<>(Map.of("id", "doc1", "text", "Conductor supports versioning", "rrfScore", 0.033)),
                new HashMap<>(Map.of("id", "doc2", "text", "Workers use HTTP polling", "rrfScore", 0.016))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?",
                "fusedContext", fusedContext
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("2 fused sources"));
        assertTrue(answer.contains("What is Conductor?"));
        assertTrue(answer.contains("Reciprocal Rank Fusion"));
        assertEquals(2, result.getOutputData().get("sourcesUsed"));
    }

    @Test
    void answerIncludesContextText() {
        List<Map<String, Object>> fusedContext = List.of(
                new HashMap<>(Map.of("id", "d1", "text", "Workflow orchestration", "rrfScore", 0.02))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "fusedContext", fusedContext
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Workflow orchestration"));
    }

    @Test
    void handlesEmptyContext() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "fusedContext", List.of()
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("No relevant context"));
        assertEquals(0, result.getOutputData().get("sourcesUsed"));
    }

    @Test
    void handlesNullContext() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("No relevant context"));
        assertEquals(0, result.getOutputData().get("sourcesUsed"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void sourcesUsedMatchesContextSize() {
        List<Map<String, Object>> fusedContext = List.of(
                new HashMap<>(Map.of("id", "a", "text", "AA", "rrfScore", 0.1)),
                new HashMap<>(Map.of("id", "b", "text", "BB", "rrfScore", 0.05)),
                new HashMap<>(Map.of("id", "c", "text", "CC", "rrfScore", 0.03))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "q",
                "fusedContext", fusedContext
        )));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("sourcesUsed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
