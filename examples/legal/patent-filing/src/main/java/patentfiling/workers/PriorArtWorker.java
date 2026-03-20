package patentfiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PriorArtWorker implements Worker {
    @Override public String getTaskDefName() { return "ptf_prior_art"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [prior-art] Searching prior art for: " + task.getInputData().get("inventionTitle"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("priorArtClear", true);
        result.getOutputData().put("matchesFound", 3);
        result.getOutputData().put("conflictRisk", "low");
        return result;
    }
}
