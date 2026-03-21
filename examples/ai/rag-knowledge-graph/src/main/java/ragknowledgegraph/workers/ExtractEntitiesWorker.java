package ragknowledgegraph.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that extracts named entities from the user's question.
 * Returns 4 fixed entities with name, type, and id fields.
 * In production this would use an NER model or LLM extraction.
 */
public class ExtractEntitiesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "kg_extract_entities";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        System.out.println("  [extract_entities] Extracting entities from: " + question);

        List<Map<String, String>> entities = List.of(
                Map.of("name", "Conductor", "type", "Software", "id", "entity-1"),
                Map.of("name", "Netflix", "type", "Organization", "id", "entity-2"),
                Map.of("name", "microservices", "type", "Architecture", "id", "entity-3"),
                Map.of("name", "workflow orchestration", "type", "Concept", "id", "entity-4")
        );

        System.out.println("  [extract_entities] Found " + entities.size() + " entities");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("entities", entities);
        return result;
    }
}
