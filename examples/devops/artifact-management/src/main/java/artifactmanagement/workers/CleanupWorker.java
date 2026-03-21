package artifactmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cleans up old artifacts beyond retention policy.
 * Input: cleanupData (publish output)
 * Output: cleanup (boolean), completedAt
 */
public class CleanupWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "am_cleanup";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [cleanup] Removed 5 old artifacts beyond retention");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("cleanup", true);
        output.put("completedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
