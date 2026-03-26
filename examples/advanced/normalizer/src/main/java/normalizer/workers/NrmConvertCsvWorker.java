package normalizer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NrmConvertCsvWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nrm_convert_csv";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [convert-csv] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("canonical", java.util.Map.of("format", "canonical", "source", "csv", "data", java.util.Map.of("rows", 10, "columns", 4)));
        return result;
    }
}