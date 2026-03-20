package npsscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class SegmentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nps_segment";
    }

    @Override
    public TaskResult execute(Task task) {
        int promoters = ((Number) task.getInputData().get("promoters")).intValue();
        int passives = ((Number) task.getInputData().get("passives")).intValue();
        int detractors = ((Number) task.getInputData().get("detractors")).intValue();
        System.out.println("  [segment] Segmenting users: promoters=" + promoters
                + " passives=" + passives + " detractors=" + detractors);

        List<Map<String, Object>> segments = List.of(
                Map.of("name", "Promoters", "count", promoters, "action", "referral_program"),
                Map.of("name", "Passives", "count", passives, "action", "engagement_boost"),
                Map.of("name", "Detractors", "count", detractors, "action", "outreach_call")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("segments", segments);
        result.getOutputData().put("totalSegments", segments.size());
        return result;
    }
}
