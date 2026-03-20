package mentalhealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class TreatmentPlanWorker implements Worker {

    @Override
    public String getTaskDefName() { return "mh_treatment_plan"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [plan] Treatment plan for " + task.getInputData().get("diagnosis"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("therapy", "Cognitive Behavioral Therapy (CBT)");
        plan.put("frequency", "weekly");
        plan.put("medication", "Sertraline 50mg daily");
        plan.put("goals", List.of("Improve sleep", "Reduce PHQ-9 below 10", "Resume daily activities"));
        plan.put("reviewDate", "2024-04-15");
        output.put("treatmentPlan", plan);
        result.setOutputData(output);
        return result;
    }
}
