package conductorui.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Step Three — Summarizes results from steps one and two.
 *
 * Input:  allData (contains stepOneResult, stepTwoResult)
 * Output: summary, path
 */
public class StepThreeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ui_step_three";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object allDataObj = task.getInputData().get("allData");

        String stepOneResult = "n/a";
        String stepTwoResult = "n/a";

        if (allDataObj instanceof Map) {
            Map<String, Object> allData = (Map<String, Object>) allDataObj;
            Object s1 = allData.get("stepOneResult");
            Object s2 = allData.get("stepTwoResult");
            if (s1 != null) stepOneResult = s1.toString();
            if (s2 != null) stepTwoResult = s2.toString();
        }

        String summary = "Pipeline complete. Step 1: " + stepOneResult + " | Step 2: " + stepTwoResult;
        String path = "ui_step_one -> ui_step_two -> ui_step_three";

        System.out.println("  [ui_step_three] " + summary);

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("summary", summary);
        taskResult.getOutputData().put("path", path);
        return taskResult;
    }
}
