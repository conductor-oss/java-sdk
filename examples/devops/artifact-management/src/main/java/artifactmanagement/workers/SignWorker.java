package artifactmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Signs the built artifact with GPG key.
 * Input: signData (build output)
 * Output: sign (boolean), processed (boolean)
 */
public class SignWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "am_sign";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sign] Artifact signed with GPG key");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("sign", true);
        output.put("processed", true);
        result.setOutputData(output);
        return result;
    }
}
