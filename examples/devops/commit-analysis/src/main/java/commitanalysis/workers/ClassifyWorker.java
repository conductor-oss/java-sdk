package commitanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class ClassifyWorker implements Worker {
    @Override public String getTaskDefName() { return "cma_classify"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> commits = (List<Map<String, Object>>) task.getInputData().getOrDefault("commits", List.of());
        Map<String, Integer> summary = new HashMap<>();
        summary.put("features", 0); summary.put("fixes", 0); summary.put("refactors", 0);
        for (Map<String, Object> c : commits) {
            String msg = (String) c.getOrDefault("message", "");
            if (msg.startsWith("feat")) summary.merge("features", 1, Integer::sum);
            else if (msg.startsWith("fix")) summary.merge("fixes", 1, Integer::sum);
            else if (msg.startsWith("refactor")) summary.merge("refactors", 1, Integer::sum);
        }
        System.out.println("  [classify] Features: " + summary.get("features") + ", Fixes: " + summary.get("fixes") + ", Refactors: " + summary.get("refactors"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("classified", commits);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
