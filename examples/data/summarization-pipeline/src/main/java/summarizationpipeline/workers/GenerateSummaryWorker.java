package summarizationpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class GenerateSummaryWorker implements Worker {
    @Override public String getTaskDefName() { return "sum_generate_summary"; }
    @Override public TaskResult execute(Task task) {
        String summary = "This paper presents advances in machine learning using a transformer architecture with attention mechanisms. The proposed approach achieves 95.2% accuracy on the benchmark dataset, representing a significant improvement over existing baselines.";
        System.out.println("  [generate] Summary generated (" + summary.split(" ").length + " words)");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("summary", summary); r.getOutputData().put("wordCount", summary.split(" ").length); r.getOutputData().put("model", "bart-large-cnn");
        return r;
    }
}
