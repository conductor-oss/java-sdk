package raglangchain.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Worker that splits documents into chunks using sentence-based splitting.
 * perform  RecursiveCharacterTextSplitter from LangChain.
 */
public class SplitTextWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lc_split_text";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> documents =
                (List<Map<String, Object>>) task.getInputData().get("documents");
        int chunkSize = task.getInputData().get("chunkSize") != null
                ? ((Number) task.getInputData().get("chunkSize")).intValue() : 200;
        int chunkOverlap = task.getInputData().get("chunkOverlap") != null
                ? ((Number) task.getInputData().get("chunkOverlap")).intValue() : 50;

        List<Map<String, Object>> chunks = new ArrayList<>();
        int chunkIndex = 0;

        if (documents != null) {
            for (Map<String, Object> doc : documents) {
                String content = (String) doc.get("pageContent");
                String[] sentences = content.split("\\. ");
                for (String sentence : sentences) {
                    String text = sentence.endsWith(".") ? sentence : sentence + ".";
                    chunks.add(Map.of(
                            "text", text,
                            "metadata", Map.of("chunkIndex", chunkIndex)
                    ));
                    chunkIndex++;
                }
            }
        }

        System.out.println("  [split_text] Split into " + chunks.size()
                + " chunks (chunkSize=" + chunkSize + ", overlap=" + chunkOverlap + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("chunks", chunks);
        result.getOutputData().put("chunkCount", chunks.size());
        result.getOutputData().put("splitter", "RecursiveCharacterTextSplitter");
        return result;
    }
}
