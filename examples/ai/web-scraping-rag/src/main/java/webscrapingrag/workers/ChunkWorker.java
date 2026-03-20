package webscrapingrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Worker that chunks scraped page content into sentence pairs.
 * Splits each page's text by ". " and groups sentences in pairs.
 */
public class ChunkWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wsrag_chunk";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> pages = (List<Map<String, Object>>) task.getInputData().get("pages");
        List<Map<String, Object>> chunks = new ArrayList<>();

        for (Map<String, Object> page : pages) {
            String text = (String) page.get("text");
            String url = (String) page.get("url");
            String[] sentences = text.split("\\. ");
            for (int i = 0; i < sentences.length; i += 2) {
                int end = Math.min(i + 2, sentences.length);
                StringBuilder sb = new StringBuilder();
                for (int j = i; j < end; j++) {
                    if (j > i) sb.append(". ");
                    sb.append(sentences[j]);
                }
                chunks.add(Map.of(
                        "id", "web-chunk-" + chunks.size(),
                        "text", sb.toString(),
                        "source", url
                ));
            }
        }

        System.out.println("  [chunk] Created " + chunks.size() + " chunks from " + pages.size() + " pages");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("chunks", chunks);
        result.getOutputData().put("chunkCount", chunks.size());
        return result;
    }
}
