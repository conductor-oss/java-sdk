package ragqualitygates.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("qg_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerWithDocuments() {
        List<Map<String, Object>> documents = List.of(
                new HashMap<>(Map.of("id", "doc-1", "text", "Doc 1", "score", 0.92)),
                new HashMap<>(Map.of("id", "doc-2", "text", "Doc 2", "score", 0.78)),
                new HashMap<>(Map.of("id", "doc-3", "text", "Doc 3", "score", 0.55))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does Conductor work?",
                "documents", documents
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("3 retrieved documents"));
        assertTrue(answer.contains("How does Conductor work?"));
    }

    @Test
    void handlesNullDocuments() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 retrieved documents"));
    }

    @Test
    void handlesNullQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void answerContainsOrchestratesContent() {
        List<Map<String, Object>> documents = List.of(
                new HashMap<>(Map.of("id", "doc-1", "text", "Doc 1", "score", 0.9))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question",
                "documents", documents
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("orchestrates"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
