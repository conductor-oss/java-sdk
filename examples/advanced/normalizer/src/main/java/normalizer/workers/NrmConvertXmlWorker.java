package normalizer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NrmConvertXmlWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nrm_convert_xml";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [convert-xml] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("canonical", java.util.Map.of("format", "canonical", "source", "xml", "data", java.util.Map.of("parsed", true, "elements", 5)));
        return result;
    }
}