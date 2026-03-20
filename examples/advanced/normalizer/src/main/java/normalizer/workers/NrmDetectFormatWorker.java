package normalizer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NrmDetectFormatWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nrm_detect_format";
    }

    @Override
    public TaskResult execute(Task task) {
        Object raw = task.getInputData().get("rawInput");
        String format = "json";
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            if (s.startsWith("<")) format = "xml";
            else if (s.contains(",") && s.contains("\n")) format = "csv";
        }
        System.out.println("  [detect] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("detectedFormat", format);
        return result;
    }
}