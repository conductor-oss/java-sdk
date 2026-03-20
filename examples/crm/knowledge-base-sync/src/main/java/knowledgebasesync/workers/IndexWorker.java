package knowledgebasesync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IndexWorker implements Worker {
    @Override public String getTaskDefName() { return "kbs_index"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [index] Search index rebuilt with " + task.getInputData().get("updatedCount") + " articles");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("indexedCount", 89);
        result.getOutputData().put("indexSizeBytes", 4521984);
        result.getOutputData().put("indexTime", "2.3s");
        return result;
    }
}
