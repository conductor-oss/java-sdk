package correctiverag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GradeRelevanceWorkerTest {

    private final GradeRelevanceWorker worker = new GradeRelevanceWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_grade_relevance", worker.getTaskDefName());
    }

    @Test
    void lowRelevanceReturnsIrrelevant() {
        List<Map<String, Object>> docs = List.of(
                new HashMap<>(Map.of("id", "d1", "text", "text1", "relevance", 0.3)),
                new HashMap<>(Map.of("id", "d2", "text", "text2", "relevance", 0.25)),
                new HashMap<>(Map.of("id", "d3", "text", "text3", "relevance", 0.2))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is pricing?",
                "documents", docs
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("irrelevant", result.getOutputData().get("verdict"));
        assertEquals("0.25", result.getOutputData().get("avgScore"));
    }

    @Test
    void highRelevanceReturnsRelevant() {
        List<Map<String, Object>> docs = List.of(
                new HashMap<>(Map.of("id", "d1", "text", "text1", "relevance", 0.9)),
                new HashMap<>(Map.of("id", "d2", "text", "text2", "relevance", 0.7)),
                new HashMap<>(Map.of("id", "d3", "text", "text3", "relevance", 0.6))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?",
                "documents", docs
        )));
        TaskResult result = worker.execute(task);

        assertEquals("relevant", result.getOutputData().get("verdict"));
    }

    @Test
    void exactThresholdReturnsRelevant() {
        List<Map<String, Object>> docs = List.of(
                new HashMap<>(Map.of("id", "d1", "text", "text1", "relevance", 0.5))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "documents", docs
        )));
        TaskResult result = worker.execute(task);

        assertEquals("relevant", result.getOutputData().get("verdict"));
        assertEquals("0.50", result.getOutputData().get("avgScore"));
    }

    @Test
    void nullDocumentsReturnsIrrelevant() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("irrelevant", result.getOutputData().get("verdict"));
        assertEquals("0.00", result.getOutputData().get("avgScore"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
