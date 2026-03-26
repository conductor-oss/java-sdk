package knowledgebasesync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class UpdateWorker implements Worker {
    @Override public String getTaskDefName() { return "kbs_update"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [update] Knowledge base " + task.getInputData().get("kbId") + ": 34 new, 55 updated, 0 deleted");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("updatedCount", 89);
        result.getOutputData().put("newArticles", 34);
        result.getOutputData().put("modifiedArticles", 55);
        return result;
    }
}
