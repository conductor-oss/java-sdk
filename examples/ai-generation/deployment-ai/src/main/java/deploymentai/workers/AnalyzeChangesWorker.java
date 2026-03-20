package deploymentai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AnalyzeChangesWorker implements Worker {
    @Override public String getTaskDefName() { return "dai_analyze_changes"; }
    @Override public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().getOrDefault("serviceName", "unknown");
        String version = (String) task.getInputData().getOrDefault("version", "0.0.0");
        Map<String, Object> analysis = Map.of("filesChanged", 8, "dbMigrations", 1, "apiChanges", 2, "configChanges", 0);
        System.out.println("  [analyze] " + serviceName + " v" + version + ": " + analysis.get("filesChanged") + " files, " + analysis.get("dbMigrations") + " migration(s)");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("changeAnalysis", analysis);
        result.getOutputData().put("changeCount", analysis.get("filesChanged"));
        return result;
    }
}
