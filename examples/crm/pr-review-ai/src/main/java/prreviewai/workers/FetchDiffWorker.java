package prreviewai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class FetchDiffWorker implements Worker {
    @Override public String getTaskDefName() { return "prr_fetch_diff"; }
    @Override public TaskResult execute(Task task) {
        Object prNumber = task.getInputData().getOrDefault("prNumber", 0);
        Map<String, Object> diff = Map.of(
            "files", List.of("src/auth.js", "src/api.js", "tests/auth.test.js"),
            "additions", 45,
            "deletions", 12
        );
        List<?> files = (List<?>) diff.get("files");
        System.out.println("  [fetch] PR #" + prNumber + ": " + files.size() + " files, +" + diff.get("additions") + "/-" + diff.get("deletions"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("diff", diff);
        result.getOutputData().put("filesChanged", files.size());
        return result;
    }
}
