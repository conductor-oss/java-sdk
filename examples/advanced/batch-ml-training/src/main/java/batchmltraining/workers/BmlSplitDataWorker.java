package batchmltraining.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BmlSplitDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bml_split_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String preparedData = (String) task.getInputData().getOrDefault("preparedData", "/data");
        System.out.println("  [split] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trainPath", preparedData + "/train");
        result.getOutputData().put("testPath", preparedData + "/test");
        result.getOutputData().put("trainSamples", 40000);
        result.getOutputData().put("testSamples", 10000);
        return result;
    }
}