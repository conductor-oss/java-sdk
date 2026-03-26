package referralmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class MatchSpecialistWorker implements Worker {

    @Override
    public String getTaskDefName() { return "ref_match_specialist"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [match] Finding " + task.getInputData().get("specialty")
                + " specialist in-network for " + task.getInputData().get("insurancePlan"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("specialistId", "DR-SPEC-3301");
        output.put("specialistName", "Dr. Rebecca Torres");
        output.put("distance", "3.2 miles");
        output.put("nextAvailable", "2024-03-25");
        result.setOutputData(output);
        return result;
    }
}
