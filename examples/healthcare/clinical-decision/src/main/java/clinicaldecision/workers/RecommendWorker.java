package clinicaldecision.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class RecommendWorker implements Worker {

    @Override
    public String getTaskDefName() { return "cds_recommend"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [recommend] Generating recommendations for risk score " + task.getInputData().get("riskScore") + "%");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        List<Map<String, Object>> recs = new ArrayList<>();
        recs.add(Map.of("priority", 1, "action", "Start high-intensity statin therapy", "evidence", "Class I, Level A"));
        recs.add(Map.of("priority", 2, "action", "Optimize blood pressure to < 130/80", "evidence", "Class I, Level A"));
        recs.add(Map.of("priority", 3, "action", "Smoking cessation counseling", "evidence", "Class I, Level A"));
        recs.add(Map.of("priority", 4, "action", "Consider low-dose aspirin", "evidence", "Class IIb, Level A"));
        output.put("recommendations", recs);
        result.setOutputData(output);
        return result;
    }
}
