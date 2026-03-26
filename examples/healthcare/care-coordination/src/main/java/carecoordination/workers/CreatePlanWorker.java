package carecoordination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Creates a care plan based on assessed needs.
 * Input: patientId, needs, condition
 * Output: careplan (goals, interventions, reviewDate)
 */
public class CreatePlanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccr_create_plan";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object needsObj = task.getInputData().get("needs");
        List<String> needs = needsObj instanceof List ? (List<String>) needsObj : List.of();

        System.out.println("  [plan] Creating care plan addressing " + needs.size() + " needs");

        Map<String, Object> careplan = Map.of(
                "goals", List.of("Stabilize blood sugar", "Improve mobility", "Address depression"),
                "interventions", needs.size(),
                "reviewDate", "2024-04-15"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("careplan", careplan);
        return result;
    }
}
