package ragknowledgegraph.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that traverses a knowledge graph starting from extracted entities.
 * Returns 7 facts (subject/predicate/object/confidence) and 4 relations
 * (from/to/type/depth). In production this would query a graph database
 * like Neo4j or Amazon Neptune.
 */
public class GraphTraverseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "kg_graph_traverse";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, String>> entities =
                (List<Map<String, String>>) task.getInputData().get("entities");
        int entityCount = (entities != null) ? entities.size() : 0;

        Object maxDepthObj = task.getInputData().get("maxDepth");
        int maxDepth = 2;
        if (maxDepthObj instanceof Number) {
            maxDepth = ((Number) maxDepthObj).intValue();
        }

        System.out.println("  [graph_traverse] Traversing graph with " + entityCount
                + " entities, maxDepth=" + maxDepth);

        List<Map<String, Object>> facts = List.of(
                Map.of("subject", "Conductor", "predicate", "developed_by", "object", "Netflix", "confidence", 0.99),
                Map.of("subject", "Conductor", "predicate", "is_a", "object", "workflow orchestration engine", "confidence", 0.98),
                Map.of("subject", "Conductor", "predicate", "supports", "object", "microservices", "confidence", 0.95),
                Map.of("subject", "Netflix", "predicate", "open_sourced", "object", "Conductor", "confidence", 0.97),
                Map.of("subject", "Conductor", "predicate", "uses", "object", "JSON workflow definitions", "confidence", 0.93),
                Map.of("subject", "microservices", "predicate", "benefit_from", "object", "workflow orchestration", "confidence", 0.91),
                Map.of("subject", "Conductor", "predicate", "provides", "object", "task queue management", "confidence", 0.89)
        );

        List<Map<String, Object>> relations = List.of(
                Map.of("from", "entity-1", "to", "entity-2", "type", "developed_by", "depth", 1),
                Map.of("from", "entity-1", "to", "entity-3", "type", "supports", "depth", 1),
                Map.of("from", "entity-1", "to", "entity-4", "type", "is_a", "depth", 1),
                Map.of("from", "entity-3", "to", "entity-4", "type", "benefit_from", "depth", 2)
        );

        System.out.println("  [graph_traverse] Found " + facts.size() + " facts, "
                + relations.size() + " relations");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("facts", facts);
        result.getOutputData().put("relations", relations);
        return result;
    }
}
