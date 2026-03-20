package releasenotesai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class CategorizeWorker implements Worker {
    @Override public String getTaskDefName() { return "rna_categorize"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, String>> commits = (List<Map<String, String>>) task.getInputData().getOrDefault("commits", List.of());
        Map<String, List<String>> categories = new HashMap<>();
        categories.put("features", new ArrayList<>());
        categories.put("fixes", new ArrayList<>());
        categories.put("docs", new ArrayList<>());
        categories.put("performance", new ArrayList<>());
        for (Map<String, String> c : commits) {
            String msg = c.getOrDefault("message", "");
            if (msg.startsWith("feat")) categories.get("features").add(msg);
            else if (msg.startsWith("fix")) categories.get("fixes").add(msg);
            else if (msg.startsWith("docs")) categories.get("docs").add(msg);
            else if (msg.startsWith("perf")) categories.get("performance").add(msg);
        }
        System.out.println("  [categorize] Features: " + categories.get("features").size() + ", Fixes: " + categories.get("fixes").size());
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("categories", categories);
        return result;
    }
}
