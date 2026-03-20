package populationhealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class InterveneWorker implements Worker {

    @Override
    public String getTaskDefName() { return "pop_intervene"; }

    @Override
    public TaskResult execute(Task task) {
        List<?> gaps = (List<?>) task.getInputData().getOrDefault("careGaps", List.of());
        System.out.println("  [intervene] Creating interventions for " + gaps.size() + " care gaps");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        List<Map<String, Object>> interventions = new ArrayList<>();
        interventions.add(Map.of("gap", "Overdue HbA1c", "action", "Outreach campaign — lab order + appointment", "targetCount", 300));
        interventions.add(Map.of("gap", "Missing eye exam", "action", "Referral letters to ophthalmology", "targetCount", 612));
        interventions.add(Map.of("gap", "Missing foot exam", "action", "In-visit reminder alerts", "targetCount", 445));
        interventions.add(Map.of("gap", "No flu vaccine", "action", "Vaccination clinic event", "targetCount", 890));
        output.put("interventions", interventions);
        output.put("totalPatientsTargeted", 2327);
        result.setOutputData(output);
        return result;
    }
}
