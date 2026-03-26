package leadnurturing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SegmentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nur_segment";
    }

    @Override
    public TaskResult execute(Task task) {
        String leadId = (String) task.getInputData().get("leadId");
        String stage = (String) task.getInputData().get("stage");
        if (stage == null) stage = "awareness";

        String segment = switch (stage) {
            case "awareness" -> "top-funnel";
            case "consideration" -> "mid-funnel";
            default -> "bottom-funnel";
        };

        System.out.println("  [segment] Lead " + leadId + " placed in " + segment);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("segment", segment);
        result.getOutputData().put("nurturePath", segment + "-track-A");
        return result;
    }
}
