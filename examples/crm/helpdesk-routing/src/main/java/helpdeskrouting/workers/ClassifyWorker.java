package helpdeskrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ClassifyWorker implements Worker {
    @Override public String getTaskDefName() { return "hdr_classify"; }

    @Override
    public TaskResult execute(Task task) {
        String desc = ((String) task.getInputData().getOrDefault("description", "")).toLowerCase();
        String tier = "tier1";
        if (desc.contains("outage") || desc.contains("security")) tier = "tier3";
        else if (desc.contains("integration") || desc.contains("api")) tier = "tier2";
        System.out.println("  [classify] Issue classified as " + tier);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tier", tier);
        result.getOutputData().put("confidence", 0.92);
        result.getOutputData().put("keywords", List.of(desc.split(" ")).subList(0, Math.min(3, desc.split(" ").length)));
        return result;
    }
}
