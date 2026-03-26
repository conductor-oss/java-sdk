package plagiarismdetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CompareWorker implements Worker {
    @Override public String getTaskDefName() { return "plg_compare"; }

    @Override public TaskResult execute(Task task) {
        int similarity = 8;
        String verdict = similarity < 20 ? "clean" : "flagged";
        System.out.println("  [compare] Similarity: " + similarity + "% -> " + verdict);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("similarityScore", similarity);
        result.getOutputData().put("verdict", verdict);
        return result;
    }
}
