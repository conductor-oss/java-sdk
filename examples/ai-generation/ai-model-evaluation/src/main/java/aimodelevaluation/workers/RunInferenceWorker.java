package aimodelevaluation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RunInferenceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ame_run_inference";
    }

    @Override
    public TaskResult execute(Task task) {

        String modelEndpoint = (String) task.getInputData().get("modelEndpoint");
        String testSamples = (String) task.getInputData().get("testSamples");
        System.out.printf("  [inference] Ran inference on %s samples%n", modelEndpoint, testSamples);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("predictions", 2000);
        result.getOutputData().put("latencyP50", 12);
        result.getOutputData().put("latencyP99", 45);
        return result;
    }
}
