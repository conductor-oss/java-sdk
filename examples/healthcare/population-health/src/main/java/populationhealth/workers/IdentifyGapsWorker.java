package populationhealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class IdentifyGapsWorker implements Worker {

    @Override
    public String getTaskDefName() { return "pop_identify_gaps"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [gaps] Identifying care gaps in population");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        List<Map<String, Object>> gaps = new ArrayList<>();
        gaps.add(Map.of("gap", "Overdue HbA1c", "affectedCount", 300, "priority", "high"));
        gaps.add(Map.of("gap", "Missing eye exam", "affectedCount", 612, "priority", "medium"));
        gaps.add(Map.of("gap", "Missing foot exam", "affectedCount", 445, "priority", "medium"));
        gaps.add(Map.of("gap", "No flu vaccine", "affectedCount", 890, "priority", "low"));
        output.put("careGaps", gaps);
        output.put("totalGaps", 4);
        result.setOutputData(output);
        return result;
    }
}
