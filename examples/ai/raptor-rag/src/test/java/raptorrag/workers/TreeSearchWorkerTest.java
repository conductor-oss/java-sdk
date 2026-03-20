package raptorrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TreeSearchWorkerTest {

    private final TreeSearchWorker worker = new TreeSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("rp_tree_search", worker.getTaskDefName());
    }

    @Test
    void returnsThreeLevelsOfContext() {
        Map<String, Object> treeSummary = Map.of(
                "id", "root-1", "text", "Root summary", "level", 2);
        List<Map<String, Object>> clusterSummaries = List.of(
                Map.of("id", "cluster-1", "text", "Cluster summary", "level", 1));
        List<Map<String, Object>> leafSummaries = List.of(
                Map.of("id", "leaf-1", "text", "Leaf summary", "level", 0));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?",
                "leafSummaries", leafSummaries,
                "clusterSummaries", clusterSummaries,
                "treeSummary", treeSummary
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> multiLevelContext =
                (List<Map<String, Object>>) result.getOutputData().get("multiLevelContext");
        assertNotNull(multiLevelContext);
        assertEquals(3, multiLevelContext.size());
    }

    @Test
    void contextHasCorrectLevels() {
        Map<String, Object> treeSummary = Map.of(
                "id", "root-1", "text", "Root summary", "level", 2);
        List<Map<String, Object>> clusterSummaries = List.of(
                Map.of("id", "cluster-1", "text", "Cluster summary", "level", 1));
        List<Map<String, Object>> leafSummaries = List.of(
                Map.of("id", "leaf-1", "text", "Leaf summary", "level", 0));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "leafSummaries", leafSummaries,
                "clusterSummaries", clusterSummaries,
                "treeSummary", treeSummary
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> multiLevelContext =
                (List<Map<String, Object>>) result.getOutputData().get("multiLevelContext");

        assertEquals(2, multiLevelContext.get(0).get("level"));
        assertEquals(1, multiLevelContext.get(1).get("level"));
        assertEquals(0, multiLevelContext.get(2).get("level"));
    }

    @Test
    void contextHasRelevanceScores() {
        Map<String, Object> treeSummary = Map.of(
                "id", "root-1", "text", "Root summary", "level", 2);
        List<Map<String, Object>> clusterSummaries = List.of(
                Map.of("id", "cluster-1", "text", "Cluster summary", "level", 1));
        List<Map<String, Object>> leafSummaries = List.of(
                Map.of("id", "leaf-1", "text", "Leaf summary", "level", 0));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "leafSummaries", leafSummaries,
                "clusterSummaries", clusterSummaries,
                "treeSummary", treeSummary
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> multiLevelContext =
                (List<Map<String, Object>>) result.getOutputData().get("multiLevelContext");

        for (Map<String, Object> ctx : multiLevelContext) {
            assertNotNull(ctx.get("relevance"));
            assertInstanceOf(Double.class, ctx.get("relevance"));
        }

        assertEquals(0.75, multiLevelContext.get(0).get("relevance"));
        assertEquals(0.88, multiLevelContext.get(1).get("relevance"));
        assertEquals(0.94, multiLevelContext.get(2).get("relevance"));
    }

    @Test
    void contextUsesTextFromInputSummaries() {
        Map<String, Object> treeSummary = Map.of(
                "id", "root-1", "text", "Custom root text", "level", 2);
        List<Map<String, Object>> clusterSummaries = List.of(
                Map.of("id", "cluster-1", "text", "Custom cluster text", "level", 1));
        List<Map<String, Object>> leafSummaries = List.of(
                Map.of("id", "leaf-1", "text", "Custom leaf text", "level", 0));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "leafSummaries", leafSummaries,
                "clusterSummaries", clusterSummaries,
                "treeSummary", treeSummary
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> multiLevelContext =
                (List<Map<String, Object>>) result.getOutputData().get("multiLevelContext");

        assertEquals("Custom root text", multiLevelContext.get(0).get("text"));
        assertEquals("Custom cluster text", multiLevelContext.get(1).get("text"));
        assertEquals("Custom leaf text", multiLevelContext.get(2).get("text"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> treeSummary = Map.of(
                "id", "root-1", "text", "Root", "level", 2);

        Task task = taskWith(new HashMap<>(Map.of("treeSummary", treeSummary)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> multiLevelContext =
                (List<Map<String, Object>>) result.getOutputData().get("multiLevelContext");
        assertNotNull(multiLevelContext);
        assertEquals(3, multiLevelContext.size());
    }

    @Test
    void handlesNullSummaries() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> multiLevelContext =
                (List<Map<String, Object>>) result.getOutputData().get("multiLevelContext");
        assertNotNull(multiLevelContext);
        assertEquals(3, multiLevelContext.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
