package namedentityextraction.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
public class LinkWorker implements Worker {
    @Override public String getTaskDefName() { return "ner_link"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> entities = (List<Map<String, Object>>) task.getInputData().getOrDefault("entities", List.of());
        Random rng = new Random();
        List<Map<String, Object>> linked = new ArrayList<>();
        for (Map<String, Object> e : entities) {
            Map<String, Object> le = new HashMap<>(e);
            le.put("wikiId", "Q" + rng.nextInt(100000));
            le.put("url", "https://en.wikipedia.org/wiki/" + ((String) e.get("text")).replace(" ", "_"));
            linked.add(le);
        }
        System.out.println("  [link] " + linked.size() + " entities linked to knowledge base");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("linkedEntities", linked);
        return result;
    }
}
