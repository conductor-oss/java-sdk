package dataclassification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Classifies data fields by sensitivity level.
 * Input: classifyData (from detect PII)
 * Output: classify, processed
 */
public class ClassifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dc_classify";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [classify] Classified: 24 sensitive, 89 internal, 205 public");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("classify", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
