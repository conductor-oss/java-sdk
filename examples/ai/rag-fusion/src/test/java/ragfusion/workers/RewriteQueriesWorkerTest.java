package ragfusion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RewriteQueriesWorkerTest {

    private final RewriteQueriesWorker worker = new RewriteQueriesWorker();

    @Test
    void taskDefName() {
        assertEquals("rf_rewrite_queries", worker.getTaskDefName());
    }

    @Test
    void returnsThreeVariantQueries() {
        Task task = taskWith(new HashMap<>(Map.of("question", "What is Conductor?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("query1"));
        assertNotNull(result.getOutputData().get("query2"));
        assertNotNull(result.getOutputData().get("query3"));
        assertEquals(3, result.getOutputData().get("variantCount"));
    }

    @Test
    void variantsContainOriginalQuestion() {
        Task task = taskWith(new HashMap<>(Map.of("question", "How does workflow versioning work?")));
        TaskResult result = worker.execute(task);

        String q1 = (String) result.getOutputData().get("query1");
        String q2 = (String) result.getOutputData().get("query2");
        String q3 = (String) result.getOutputData().get("query3");

        assertTrue(q1.contains("How does workflow versioning work?"));
        assertTrue(q2.contains("How does workflow versioning work?"));
        assertTrue(q3.contains("How does workflow versioning work?"));
    }

    @Test
    void variantsAreDifferent() {
        Task task = taskWith(new HashMap<>(Map.of("question", "What is Conductor?")));
        TaskResult result = worker.execute(task);

        String q1 = (String) result.getOutputData().get("query1");
        String q2 = (String) result.getOutputData().get("query2");
        String q3 = (String) result.getOutputData().get("query3");

        assertNotEquals(q1, q2);
        assertNotEquals(q2, q3);
        assertNotEquals(q1, q3);
    }

    @Test
    void handlesEmptyQuestion() {
        Task task = taskWith(new HashMap<>(Map.of("question", "")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("variantCount"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("query1"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
