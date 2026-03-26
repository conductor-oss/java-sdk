package namedentityextraction.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ExtractEntitiesWorker implements Worker {
    @Override public String getTaskDefName() { return "ner_extract_entities"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> entities = List.of(
            Map.of("text", "Apple Inc.", "type", "ORGANIZATION", "start", 0, "end", 1),
            Map.of("text", "Tim Cook", "type", "PERSON", "start", 3, "end", 4),
            Map.of("text", "Cupertino", "type", "LOCATION", "start", 6, "end", 6),
            Map.of("text", "California", "type", "LOCATION", "start", 8, "end", 8)
        );
        System.out.println("  [extract] Extracted " + entities.size() + " entities");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("entities", entities);
        result.getOutputData().put("entityCount", entities.size());
        return result;
    }
}
