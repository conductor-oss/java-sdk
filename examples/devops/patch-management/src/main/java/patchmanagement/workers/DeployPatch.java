package patchmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deploys a patch to affected hosts in rolling fashion.
 * Input: deploy_patchData (from test output)
 * Output: deployed, hostsPatched, deploymentStrategy, processed
 */
public class DeployPatch implements Worker {

    @Override
    public String getTaskDefName() {
        return "pm_deploy_patch";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("deploy_patchData");
        boolean stagingPassed = true;
        if (data != null && data.get("stagingPassed") != null) {
            stagingPassed = Boolean.TRUE.equals(data.get("stagingPassed"));
        }

        System.out.println("[pm_deploy_patch] Patch deployed to hosts in rolling fashion (staging passed: " + stagingPassed + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("deployed", true);
        output.put("hostsPatched", 24);
        output.put("deploymentStrategy", "rolling");
        output.put("processed", true);
        result.setOutputData(output);
        return result;
    }
}
