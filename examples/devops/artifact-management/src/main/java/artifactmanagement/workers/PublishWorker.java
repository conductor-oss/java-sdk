package artifactmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes the signed artifact to Artifactory.
 * Input: publishData (sign output)
 * Output: publish (boolean), processed (boolean)
 */
public class PublishWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "am_publish";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [publish] Published to Artifactory");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("publish", true);
        output.put("processed", true);
        result.setOutputData(output);
        return result;
    }
}
