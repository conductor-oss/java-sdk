package deploymentai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class PredictRiskWorker implements Worker {
    @Override public String getTaskDefName() { return "dai_predict_risk"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> ca = (Map<String, Object>) task.getInputData().getOrDefault("changeAnalysis", Map.of());
        int dbMigrations = ca.get("dbMigrations") instanceof Number ? ((Number) ca.get("dbMigrations")).intValue() : 0;
        int apiChanges = ca.get("apiChanges") instanceof Number ? ((Number) ca.get("apiChanges")).intValue() : 0;
        String riskLevel = dbMigrations > 0 ? "medium" : apiChanges > 0 ? "low" : "minimal";
        System.out.println("  [risk] Predicted risk: " + riskLevel);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("riskLevel", riskLevel);
        result.getOutputData().put("factors", List.of("db_migration", "api_changes"));
        return result;
    }
}
