package namedentityextraction.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class TokenizeWorker implements Worker {
    @Override public String getTaskDefName() { return "ner_tokenize"; }
    @Override public TaskResult execute(Task task) {
        String text = (String) task.getInputData().getOrDefault("text", "");
        String[] words = text.split("\\s+");
        List<Map<String, Object>> tokens = new ArrayList<>();
        for (int i = 0; i < words.length; i++) tokens.add(Map.of("index", i, "token", words[i]));
        System.out.println("  [tokenize] Text tokenized into " + tokens.size() + " tokens");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tokens", tokens);
        result.getOutputData().put("tokenCount", tokens.size());
        return result;
    }
}
