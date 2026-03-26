package raglangchain.workers;

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
        assertEquals("lc_generate", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatesAnswerWithSources() {
        List<Map<String, Object>> retrievedDocs = List.of(
                Map.of("text", "LangChain is a framework.", "score", 0.95,
                        "metadata", Map.of("chunkIndex", 0)),
                Map.of("text", "RAG combines retrieval with generation.", "score", 0.87,
                        "metadata", Map.of("chunkIndex", 1))
        );

        Task task = taskWith(Map.of(
                "question", "What is LangChain?",
                "retrievedDocs", retrievedDocs
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("LangChain"));

        List<Map<String, Object>> sources =
                (List<Map<String, Object>>) result.getOutputData().get("sources");
        assertNotNull(sources);
        assertEquals(2, sources.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sourcesHaveChunkScoreAndPage() {
        List<Map<String, Object>> retrievedDocs = List.of(
                Map.of("text", "Some content.", "score", 0.95,
                        "metadata", Map.of("chunkIndex", 0)),
                Map.of("text", "More content.", "score", 0.87,
                        "metadata", Map.of("chunkIndex", 2))
        );

        Task task = taskWith(Map.of(
                "question", "Test question?",
                "retrievedDocs", retrievedDocs
        ));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> sources =
                (List<Map<String, Object>>) result.getOutputData().get("sources");
        Map<String, Object> firstSource = sources.get(0);
        assertEquals("Some content.", firstSource.get("chunk"));
        assertEquals(0.95, firstSource.get("score"));
        assertEquals(1, firstSource.get("page"));

        Map<String, Object> secondSource = sources.get(1);
        assertEquals(3, secondSource.get("page"));
    }

    @Test
    void returnsChainAndTokensUsed() {
        List<Map<String, Object>> retrievedDocs = List.of(
                Map.of("text", "Content.", "score", 0.95,
                        "metadata", Map.of("chunkIndex", 0))
        );

        Task task = taskWith(Map.of(
                "question", "What is RAG?",
                "retrievedDocs", retrievedDocs
        ));
        TaskResult result = worker.execute(task);

        assertEquals("RetrievalQA", result.getOutputData().get("chain"));
        assertEquals(215, result.getOutputData().get("tokensUsed"));
    }

    @Test
    void handlesDefaultQuestion() {
        List<Map<String, Object>> retrievedDocs = List.of(
                Map.of("text", "Content.", "score", 0.95,
                        "metadata", Map.of("chunkIndex", 0))
        );

        Task task = taskWith(Map.of("retrievedDocs", retrievedDocs));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
