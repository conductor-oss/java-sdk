package releasenotesai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class CollectCommitsWorker implements Worker {
    @Override public String getTaskDefName() { return "rna_collect_commits"; }
    @Override public TaskResult execute(Task task) {
        String fromTag = (String) task.getInputData().getOrDefault("fromTag", "v0.0.0");
        String toTag = (String) task.getInputData().getOrDefault("toTag", "v0.0.1");
        List<Map<String, String>> commits = List.of(
            Map.of("hash", "a1b2c3", "message", "feat: add user dashboard", "author", "alice"),
            Map.of("hash", "d4e5f6", "message", "fix: resolve login timeout", "author", "bob"),
            Map.of("hash", "g7h8i9", "message", "docs: update API reference", "author", "carol"),
            Map.of("hash", "j0k1l2", "message", "perf: optimize query caching", "author", "dave")
        );
        System.out.println("  [collect] Found " + commits.size() + " commits from " + fromTag + " to " + toTag);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("commits", commits);
        result.getOutputData().put("commitCount", commits.size());
        return result;
    }
}
