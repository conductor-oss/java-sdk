package questionanswering.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ParseQuestionWorker implements Worker {
    @Override public String getTaskDefName() { return "qas_parse_question"; }
    @Override public TaskResult execute(Task task) {
        String q = (String) task.getInputData().getOrDefault("question", "");
        String type = q.startsWith("How") ? "procedural" : q.startsWith("What") ? "factual" : q.startsWith("Why") ? "explanatory" : "general";
        System.out.println("  [parse] Question type: " + type + ", keywords extracted");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("parsed", Map.of("original", q, "type", type, "keywords", List.of("Conductor", "workflow", "timeout")));
        r.getOutputData().put("questionType", type);
        return r;
    }
}
