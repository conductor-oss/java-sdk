package raptorrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that builds cluster-level summaries from leaf summaries.
 * Produces 2 cluster summaries at level 1 and a single tree summary
 * at level 2 (root) of the RAPTOR tree.
 */
public class ClusterSummariesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rp_cluster_summaries";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> leafSummaries =
                (List<Map<String, Object>>) task.getInputData().get("leafSummaries");
        int leafCount = (leafSummaries != null) ? leafSummaries.size() : 0;

        System.out.println("  [cluster_summaries] Clustering " + leafCount + " leaf summaries");

        // Fixed deterministic cluster summaries at level 1
        List<Map<String, Object>> clusterSummaries = List.of(
                Map.of("id", "cluster-1", "text",
                        "Conductor provides DAG-based orchestration with polyglot worker support for building distributed applications.",
                        "leafIds", List.of("leaf-1", "leaf-2"),
                        "level", 1),
                Map.of("id", "cluster-2", "text",
                        "The platform ensures reliability through retries, versioning, and reactive event-driven workflows.",
                        "leafIds", List.of("leaf-3", "leaf-4"),
                        "level", 1)
        );

        // Fixed deterministic tree summary at level 2 (root)
        Map<String, Object> treeSummary = Map.of(
                "id", "root-1",
                "text", "Conductor is a distributed orchestration platform offering DAG workflows, polyglot workers, reliability features, and event-driven automation.",
                "clusterIds", List.of("cluster-1", "cluster-2"),
                "level", 2
        );

        System.out.println("  [cluster_summaries] Created " + clusterSummaries.size()
                + " clusters (level 1) + 1 tree summary (level 2)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("clusterSummaries", clusterSummaries);
        result.getOutputData().put("treeSummary", treeSummary);
        return result;
    }
}
