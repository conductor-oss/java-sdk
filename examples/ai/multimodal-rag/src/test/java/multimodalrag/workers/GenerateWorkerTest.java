package multimodalrag.workers;

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
        assertEquals("mm_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerWithAllModalities() {
        List<Map<String, Object>> searchResults = List.of(
                new HashMap<>(Map.of("resultId", "res-001", "modality", "text", "score", 0.95, "content", "text result")),
                new HashMap<>(Map.of("resultId", "res-002", "modality", "image", "score", 0.89, "content", "image result")),
                new HashMap<>(Map.of("resultId", "res-003", "modality", "audio", "score", 0.82, "content", "audio result")),
                new HashMap<>(Map.of("resultId", "res-004", "modality", "text", "score", 0.78, "content", "text result 2"))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does multimodal retrieval work?",
                "searchResults", searchResults,
                "modalities", List.of("text", "image", "audio")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("How does multimodal retrieval work?"));
        assertTrue(answer.contains("3 modalities"));
        assertTrue(answer.contains("4 relevant results"));
    }

    @Test
    void answerMentionsModalities() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question",
                "searchResults", List.of(
                        new HashMap<>(Map.of("resultId", "res-001", "content", "result"))
                ),
                "modalities", List.of("text", "image")
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("text"));
        assertTrue(answer.contains("image"));
    }

    @Test
    void handlesNullQuestion() {
        Task task = taskWith(new HashMap<>(Map.of(
                "searchResults", List.of(),
                "modalities", List.of("text")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesNullSearchResults() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "modalities", List.of("text")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 relevant results"));
    }

    @Test
    void handlesNullModalities() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 modalities"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(new HashMap<>());
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
