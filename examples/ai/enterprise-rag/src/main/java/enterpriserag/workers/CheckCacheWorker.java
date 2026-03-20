package enterpriserag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that checks a cache for a previously answered question.
 * Takes question and userId, returns cacheStatus and cachedAnswer.
 */
public class CheckCacheWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "er_check_cache";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        String userId = (String) task.getInputData().get("userId");

        System.out.println("  [check_cache] Checking cache for user=" + userId
                + " question=\"" + question + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cacheStatus", "miss");
        result.getOutputData().put("cachedAnswer", null);
        return result;
    }
}
