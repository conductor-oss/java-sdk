package nutritiontracking;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import nutritiontracking.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 739: Nutrition Tracking — Log Meal, Lookup Nutrition, Calculate Daily, Report
 */
public class NutritionTrackingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 739: Nutrition Tracking ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("nut_log_meal", "nut_lookup_nutrition", "nut_calculate_daily", "nut_report"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new LogMealWorker(), new LookupNutritionWorker(), new CalculateDailyWorker(), new ReportWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("nutrition_tracking_739", 1,
                Map.of("userId", "USR-55", "mealType", "lunch", "foods", List.of("Grilled Chicken", "Brown Rice", "Broccoli")));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
