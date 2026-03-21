package dischargeplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Creates a discharge plan based on readiness assessment.
 * Input: patientId, readiness, needs
 * Output: services, medications, followUpNeeds
 */
public class CreateDischargePlanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dsc_create_plan";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object needsObj = task.getInputData().get("needs");
        List<?> needs = needsObj instanceof List ? (List<?>) needsObj : List.of();

        System.out.println("  [plan] Creating discharge plan with " + needs.size() + " needs");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("services", List.of("home_health", "pharmacy_delivery", "pt_outpatient"));
        result.getOutputData().put("medications", List.of(
                Map.of("name", "Metoprolol", "dosage", "25mg", "frequency", "twice daily"),
                Map.of("name", "Aspirin", "dosage", "81mg", "frequency", "daily")
        ));
        result.getOutputData().put("followUpNeeds", List.of("cardiology_2wk", "pcp_1wk"));
        return result;
    }
}
