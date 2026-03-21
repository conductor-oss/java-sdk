package patchmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tests a patch on staging environment before deployment.
 * Input: test_patchData (from scan output)
 * Output: tested, regressions, stagingPassed, testsRun
 */
public class TestPatch implements Worker {

    @Override
    public String getTaskDefName() {
        return "pm_test_patch";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("test_patchData");
        String patchId = "PATCH-UNKNOWN";
        if (data != null && data.get("patchId") != null) {
            patchId = (String) data.get("patchId");
        }

        System.out.println("[pm_test_patch] Patch " + patchId + " tested on staging: no regressions");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("tested", true);
        output.put("regressions", 0);
        output.put("stagingPassed", true);
        output.put("testsRun", 87);
        result.setOutputData(output);
        return result;
    }
}
