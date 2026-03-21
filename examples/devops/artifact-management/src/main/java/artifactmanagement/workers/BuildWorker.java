package artifactmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the project artifact.
 * Input: project, version
 * Output: buildId, success
 */
public class BuildWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "am_build";
    }

    @Override
    public TaskResult execute(Task task) {
        String project = (String) task.getInputData().getOrDefault("project", "unknown");
        String version = (String) task.getInputData().getOrDefault("version", "0.0.0");

        System.out.println("  [build] Built " + project + ":" + version);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("buildId", "BUILD-1355");
        output.put("success", true);
        result.setOutputData(output);
        return result;
    }
}
