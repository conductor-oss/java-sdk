package ragmongodb.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MongoGenerateWorkerTest {

    private final MongoGenerateWorker worker = new MongoGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("mongo_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerFromDocs() {
        List<Object> docs = new ArrayList<>(List.of(
                new HashMap<>(Map.of("title", "Doc 1", "content", "Content 1")),
                new HashMap<>(Map.of("title", "Doc 2", "content", "Content 2")),
                new HashMap<>(Map.of("title", "Doc 3", "content", "Content 3"))
        ));
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does MongoDB Atlas Vector Search work?",
                "docs", docs
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("$vectorSearch"));
        assertTrue(answer.contains("3 retrieved docs"));
    }

    @Test
    void answerContainsDocCount() {
        List<Object> docs = new ArrayList<>(List.of(
                new HashMap<>(Map.of("title", "Single Doc", "content", "Only one"))
        ));
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question",
                "docs", docs
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("1 retrieved docs"));
    }

    @Test
    void answerMentionsVectorSearch() {
        List<Object> docs = new ArrayList<>(List.of(
                new HashMap<>(Map.of("title", "A", "content", "B")),
                new HashMap<>(Map.of("title", "C", "content", "D"))
        ));
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is vector search?",
                "docs", docs
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("MongoDB Atlas Vector Search"));
        assertTrue(answer.contains("embedding field"));
        assertTrue(answer.contains("numCandidates"));
    }

    @Test
    void handlesEmptyDocs() {
        List<Object> docs = new ArrayList<>();
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "docs", docs
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 retrieved docs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
