package npsscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class ActWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nps_act";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        int npsScore = ((Number) task.getInputData().get("npsScore")).intValue();
        List<Map<String, Object>> segments = (List<Map<String, Object>>) task.getInputData().get("segments");
        System.out.println("  [act] Triggering actions based on NPS score " + npsScore
                + " across " + (segments != null ? segments.size() : 0) + " segments");

        List<Map<String, Object>> actionsTriggered = List.of(
                Map.of("segment", "Promoters", "action", "referral_program", "status", "triggered"),
                Map.of("segment", "Passives", "action", "engagement_boost", "status", "triggered"),
                Map.of("segment", "Detractors", "action", "outreach_call", "status", "triggered")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("actionsTriggered", actionsTriggered);
        result.getOutputData().put("totalActions", actionsTriggered.size());
        return result;
    }
}
