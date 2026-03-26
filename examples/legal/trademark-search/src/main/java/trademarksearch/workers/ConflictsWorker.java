package trademarksearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConflictsWorker implements Worker {
    @Override public String getTaskDefName() { return "tmk_conflicts"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [conflicts] Analyzing conflicts from search results");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("conflicts", java.util.List.of(java.util.Map.of("mark", "SimilarMark-001", "similarity", 0.65)));
        result.getOutputData().put("conflictCount", 1);
        return result;
    }
}
