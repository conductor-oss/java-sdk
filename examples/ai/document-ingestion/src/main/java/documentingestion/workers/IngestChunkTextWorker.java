package documentingestion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker 2: Splits extracted text into overlapping chunks.
 * Uses word-based chunking with configurable size and overlap.
 */
public class IngestChunkTextWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ingest_chunk_text";
    }

    @Override
    public TaskResult execute(Task task) {
        String text = (String) task.getInputData().get("text");

        int chunkSize = 200;
        int chunkOverlap = 50;

        Object chunkSizeInput = task.getInputData().get("chunkSize");
        if (chunkSizeInput != null) {
            chunkSize = Integer.parseInt(chunkSizeInput.toString());
        }

        Object chunkOverlapInput = task.getInputData().get("chunkOverlap");
        if (chunkOverlapInput != null) {
            chunkOverlap = Integer.parseInt(chunkOverlapInput.toString());
        }

        String[] words = text.split("\\s+");
        List<Map<String, Object>> chunks = new ArrayList<>();
        int start = 0;

        while (start < words.length) {
            int end = Math.min(start + chunkSize, words.length);
            StringBuilder chunkText = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (i > start) chunkText.append(" ");
                chunkText.append(words[i]);
            }

            Map<String, Object> chunk = new LinkedHashMap<>();
            chunk.put("id", "chunk-" + chunks.size());
            chunk.put("text", chunkText.toString());
            chunk.put("wordCount", end - start);
            chunk.put("startOffset", start);
            chunks.add(chunk);

            start += chunkSize - chunkOverlap;
            if (start >= words.length) break;
        }

        System.out.println("  [chunk] Split into " + chunks.size() + " chunks (size=" + chunkSize + " words, overlap=" + chunkOverlap + ")");
        for (Map<String, Object> c : chunks) {
            String cText = (String) c.get("text");
            String preview = cText.length() > 50 ? cText.substring(0, 50) + "..." : cText;
            System.out.println("    - " + c.get("id") + ": " + c.get("wordCount") + " words, \"" + preview + "\"");
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("chunks", chunks);
        result.getOutputData().put("chunkCount", chunks.size());
        return result;
    }
}
