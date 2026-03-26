package raptorrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClusterSummariesWorkerTest {

    private final ClusterSummariesWorker worker = new ClusterSummariesWorker();

    @Test
    void taskDefName() {
        assertEquals("rp_cluster_summaries", worker.getTaskDefName());
    }

    @Test
    void returnsTwoClusterSummaries() {
        List<Map<String, Object>> leafSummaries = List.of(
                Map.of("id", "leaf-1", "text", "Summary 1", "level", 0),
                Map.of("id", "leaf-2", "text", "Summary 2", "level", 0)
        );

        Task task = taskWith(new HashMap<>(Map.of("leafSummaries", leafSummaries)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> clusterSummaries =
                (List<Map<String, Object>>) result.getOutputData().get("clusterSummaries");
        assertNotNull(clusterSummaries);
        assertEquals(2, clusterSummaries.size());
    }

    @Test
    void clusterSummariesAreLevel1() {
        Task task = taskWith(new HashMap<>(Map.of("leafSummaries", List.of())));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> clusterSummaries =
                (List<Map<String, Object>>) result.getOutputData().get("clusterSummaries");

        for (Map<String, Object> cluster : clusterSummaries) {
            assertEquals(1, cluster.get("level"));
            assertNotNull(cluster.get("id"));
            assertNotNull(cluster.get("text"));
            assertNotNull(cluster.get("leafIds"));
            assertInstanceOf(List.class, cluster.get("leafIds"));
        }
    }

    @Test
    void returnsTreeSummaryAtLevel2() {
        Task task = taskWith(new HashMap<>(Map.of("leafSummaries", List.of())));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> treeSummary =
                (Map<String, Object>) result.getOutputData().get("treeSummary");
        assertNotNull(treeSummary);
        assertEquals(2, treeSummary.get("level"));
        assertEquals("root-1", treeSummary.get("id"));
        assertNotNull(treeSummary.get("text"));
        assertNotNull(treeSummary.get("clusterIds"));
    }

    @Test
    void treeSummaryReferencesClusterIds() {
        Task task = taskWith(new HashMap<>(Map.of("leafSummaries", List.of())));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> treeSummary =
                (Map<String, Object>) result.getOutputData().get("treeSummary");

        @SuppressWarnings("unchecked")
        List<String> clusterIds = (List<String>) treeSummary.get("clusterIds");
        assertTrue(clusterIds.contains("cluster-1"));
        assertTrue(clusterIds.contains("cluster-2"));
    }

    @Test
    void clusterSummariesReferenceLeafIds() {
        Task task = taskWith(new HashMap<>(Map.of("leafSummaries", List.of())));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> clusterSummaries =
                (List<Map<String, Object>>) result.getOutputData().get("clusterSummaries");

        @SuppressWarnings("unchecked")
        List<String> leafIds = (List<String>) clusterSummaries.get(0).get("leafIds");
        assertTrue(leafIds.contains("leaf-1"));
        assertTrue(leafIds.contains("leaf-2"));
    }

    @Test
    void handlesNullLeafSummaries() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("clusterSummaries"));
        assertNotNull(result.getOutputData().get("treeSummary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
