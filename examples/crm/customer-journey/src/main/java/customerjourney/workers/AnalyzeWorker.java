package customerjourney.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Analyzes the customer journey for insights.
 * Input: journeyMap
 * Output: insights (avgConversionDays, dropOffStage, topChannel, conversionRate)
 */
public class AnalyzeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cjy_analyze";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [analyze] Journey analysis: avg 10 days from awareness to purchase");

        Map<String, Object> insights = Map.of(
                "avgConversionDays", 10,
                "dropOffStage", "consideration",
                "topChannel", "email",
                "conversionRate", "12.4%"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("insights", insights);
        return result;
    }
}
