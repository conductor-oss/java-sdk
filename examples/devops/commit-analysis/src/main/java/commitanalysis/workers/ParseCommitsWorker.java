package commitanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ParseCommitsWorker implements Worker {
    @Override public String getTaskDefName() { return "cma_parse_commits"; }
    @Override public TaskResult execute(Task task) {
        String branch = (String) task.getInputData().getOrDefault("branch", "main");
        Object days = task.getInputData().getOrDefault("days", 30);
        List<Map<String, Object>> commits = List.of(
            Map.of("hash", "abc123", "message", "feat: add search", "files", 5, "author", "alice"),
            Map.of("hash", "def456", "message", "fix: null pointer in auth", "files", 2, "author", "bob"),
            Map.of("hash", "ghi789", "message", "refactor: extract service layer", "files", 12, "author", "alice"),
            Map.of("hash", "jkl012", "message", "fix: race condition in cache", "files", 3, "author", "carol"),
            Map.of("hash", "mno345", "message", "feat: add notifications", "files", 8, "author", "bob")
        );
        System.out.println("  [parse] Found " + commits.size() + " commits on " + branch + " (last " + days + " days)");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("commits", commits);
        result.getOutputData().put("commitCount", commits.size());
        return result;
    }
}
