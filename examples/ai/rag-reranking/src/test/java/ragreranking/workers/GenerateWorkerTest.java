package ragreranking.workers;

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
        assertEquals("rerank_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerUsingContextSize() {
        List<Map<String, Object>> context = new ArrayList<>();
        context.add(new HashMap<>(Map.of("id", "doc-B", "text", "Re-ranking improves RAG precision.",
                "crossEncoderScore", 0.97)));
        context.add(new HashMap<>(Map.of("id", "doc-A", "text", "Cross-encoder models score query-document pairs.",
                "crossEncoderScore", 0.94)));
        context.add(new HashMap<>(Map.of("id", "doc-F", "text", "Cohere Rerank API is a popular reranking model.",
                "crossEncoderScore", 0.91)));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does re-ranking improve RAG accuracy?",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("3 re-ranked sources"));
    }

    @Test
    void answerContainsExpectedContent() {
        List<Map<String, Object>> context = new ArrayList<>();
        context.add(new HashMap<>(Map.of("id", "doc-1", "text", "Text.", "crossEncoderScore", 0.9)));
        context.add(new HashMap<>(Map.of("id", "doc-2", "text", "More text.", "crossEncoderScore", 0.8)));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does re-ranking work?",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Re-ranking"));
        assertTrue(answer.contains("cross-encoder"));
        assertTrue(answer.contains("2 re-ranked sources"));
    }

    @Test
    void handlesNullContext() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 re-ranked sources"));
    }

    @Test
    void handlesSingleDocument() {
        List<Map<String, Object>> context = new ArrayList<>();
        context.add(new HashMap<>(Map.of("id", "doc-1", "text", "Text.", "crossEncoderScore", 0.95)));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("1 re-ranked sources"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
