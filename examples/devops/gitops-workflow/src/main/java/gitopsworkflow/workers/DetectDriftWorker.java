package gitopsworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Detects configuration drift between Git repository and target cluster.
 * Input: repository, targetCluster
 * Output: detect_driftId, success
 */
public class DetectDriftWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "go_detect_drift";
    }

    @Override
    public TaskResult execute(Task task) {
        String repository = (String) task.getInputData().get("repository");
        String targetCluster = (String) task.getInputData().get("targetCluster");

        if (repository == null) repository = "unknown-repo";
        if (targetCluster == null) targetCluster = "unknown-cluster";

        System.out.println("  [drift] Detected 3 drifted resources in " + targetCluster);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("detect_driftId", "DETECT_DRIFT-1361");
        result.getOutputData().put("success", true);
        result.getOutputData().put("repository", repository);
        result.getOutputData().put("targetCluster", targetCluster);
        result.getOutputData().put("driftedResources", 3);
        return result;
    }
}
