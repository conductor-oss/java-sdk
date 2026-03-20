package namedentityextraction.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class TagWorker implements Worker {
    @Override public String getTaskDefName() { return "ner_tag"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, String>> tagged = List.of(
            Map.of("token", "Apple", "tag", "B-ORG"), Map.of("token", "Inc.", "tag", "I-ORG"),
            Map.of("token", "announced", "tag", "O"),
            Map.of("token", "Tim", "tag", "B-PER"), Map.of("token", "Cook", "tag", "I-PER"),
            Map.of("token", "in", "tag", "O"),
            Map.of("token", "Cupertino", "tag", "B-LOC"), Map.of("token", ",", "tag", "O"),
            Map.of("token", "California", "tag", "B-LOC")
        );
        long entityTokens = tagged.stream().filter(t -> !"O".equals(t.get("tag"))).count();
        System.out.println("  [tag] " + entityTokens + " tokens tagged with entity labels");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("taggedTokens", tagged);
        return result;
    }
}
