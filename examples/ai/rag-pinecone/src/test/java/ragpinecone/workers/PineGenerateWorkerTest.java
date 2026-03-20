package ragpinecone.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PineGenerateWorkerTest {

    private final PineGenerateWorker worker = new PineGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("pine_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerFromMatches() {
        List<Map<String, Object>> matches = new ArrayList<>();
        matches.add(new HashMap<>(Map.of(
                "id", "vec-001",
                "score", 0.96,
                "metadata", new HashMap<>(Map.of(
                        "text", "Pinecone is a managed vector database for ML applications.",
                        "category", "technical"
                ))
        )));
        matches.add(new HashMap<>(Map.of(
                "id", "vec-002",
                "score", 0.91,
                "metadata", new HashMap<>(Map.of(
                        "text", "Serverless indexes scale automatically based on usage.",
                        "category", "technical"
                ))
        )));

        Task task = taskWith(Map.of(
                "question", "What is Pinecone?",
                "matches", matches
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("What is Pinecone?"));
        assertTrue(answer.contains("Pinecone is a managed vector database"));
        assertTrue(answer.contains("Serverless indexes scale automatically"));
    }

    @Test
    void answerContainsAllThreeMatchTexts() {
        List<Map<String, Object>> matches = new ArrayList<>();
        matches.add(new HashMap<>(Map.of(
                "id", "vec-001", "score", 0.96,
                "metadata", new HashMap<>(Map.of("text", "First passage.", "category", "tech"))
        )));
        matches.add(new HashMap<>(Map.of(
                "id", "vec-002", "score", 0.91,
                "metadata", new HashMap<>(Map.of("text", "Second passage.", "category", "tech"))
        )));
        matches.add(new HashMap<>(Map.of(
                "id", "vec-003", "score", 0.87,
                "metadata", new HashMap<>(Map.of("text", "Third passage.", "category", "tech"))
        )));

        Task task = taskWith(Map.of("question", "Test?", "matches", matches));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("First passage."));
        assertTrue(answer.contains("Second passage."));
        assertTrue(answer.contains("Third passage."));
    }

    @Test
    void handlesEmptyMatches() {
        Task task = taskWith(Map.of(
                "question", "What is Pinecone?",
                "matches", new ArrayList<>()
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("What is Pinecone?"));
    }

    @Test
    void defaultsQuestionWhenMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "matches", new ArrayList<>()
        )));
        task.getInputData().put("question", null);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("What is Pinecone?"));
    }

    @Test
    void answerStartsWithBasedOn() {
        List<Map<String, Object>> matches = new ArrayList<>();
        matches.add(new HashMap<>(Map.of(
                "id", "vec-001", "score", 0.96,
                "metadata", new HashMap<>(Map.of("text", "Some text.", "category", "tech"))
        )));

        Task task = taskWith(Map.of("question", "How does it work?", "matches", matches));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Based on How does it work?:"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
