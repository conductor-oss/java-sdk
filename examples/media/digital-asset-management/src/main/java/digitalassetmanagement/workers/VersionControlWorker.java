package digitalassetmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Creates a version entry for the asset.
 * Input: assetId, projectId, checksum
 * Output: version, previousVersion, changeType, versionId
 */
public class VersionControlWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dam_version_control";
    }

    @Override
    public TaskResult execute(Task task) {
        String projectId = (String) task.getInputData().get("projectId");
        if (projectId == null) projectId = "unknown";

        System.out.println("  [version] Creating version for project " + projectId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("version", "1.0.0");
        result.getOutputData().put("previousVersion", null);
        result.getOutputData().put("changeType", "initial");
        result.getOutputData().put("versionId", "VER-521-001");
        return result;
    }
}
