package aimodelevaluation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PrepareTestSetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ame_prepare_test_set";
    }

    @Override
    public TaskResult execute(Task task) {

        String testDatasetId = (String) task.getInputData().get("testDatasetId");
        String taskType = (String) task.getInputData().get("taskType");
        System.out.printf("  [prepare] Test set %s prepared — 2000 samples%n", testDatasetId, taskType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sampleCount", 2000);
        result.getOutputData().put("classes", 10);
        return result;
    }
}
