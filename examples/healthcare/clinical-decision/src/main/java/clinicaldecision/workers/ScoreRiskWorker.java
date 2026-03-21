package clinicaldecision.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScoreRiskWorker implements Worker {

    @Override
    public String getTaskDefName() { return "cds_score_risk"; }

    @Override
    public TaskResult execute(Task task) {
        double riskScore = 22.5;
        String category = riskScore >= 20 ? "high" : riskScore >= 7.5 ? "moderate" : "low";
        System.out.println("  [risk] 10-year ASCVD risk: " + riskScore + "% (" + category + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("riskScore", riskScore);
        output.put("riskCategory", category);
        output.put("model", "Pooled Cohort Equations");
        result.setOutputData(output);
        return result;
    }
}
