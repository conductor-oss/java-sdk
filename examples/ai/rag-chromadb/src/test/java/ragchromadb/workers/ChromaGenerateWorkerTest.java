package ragchromadb.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChromaGenerateWorkerTest {

    private final ChromaGenerateWorker worker = new ChromaGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("chroma_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerFromResults() {
        Map<String, Object> results = new HashMap<>(Map.of(
                "documents", List.of(List.of(
                        "ChromaDB is an open-source embedding database designed for AI applications.",
                        "Collections in ChromaDB store embeddings with optional metadata and documents.",
                        "ChromaDB supports persistent storage and runs locally or via Docker."
                ))
        ));
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does ChromaDB store embeddings?",
                "results", results
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("ChromaDB"));
        assertTrue(answer.contains("3 retrieved chunks"));
    }

    @Test
    void answerContainsChunkCount() {
        Map<String, Object> results = new HashMap<>(Map.of(
                "documents", List.of(List.of("Doc 1", "Doc 2"))
        ));
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question",
                "results", results
        )));

        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("2 retrieved chunks"));
    }

    @Test
    void answerMentionsKeyTopics() {
        Map<String, Object> results = new HashMap<>(Map.of(
                "documents", List.of(List.of("doc1", "doc2", "doc3"))
        ));
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is ChromaDB?",
                "results", results
        )));

        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("open-source"));
        assertTrue(answer.contains("embedding database"));
        assertTrue(answer.contains("Docker"));
    }

    @Test
    void outputContainsAnswerField() {
        Map<String, Object> results = new HashMap<>(Map.of(
                "documents", List.of(List.of("single doc"))
        ));
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "results", results
        )));

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
