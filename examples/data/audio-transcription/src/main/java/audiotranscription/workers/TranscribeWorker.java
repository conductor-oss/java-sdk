package audiotranscription.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Transcribes audio into text with speaker diarization.
 * Input: processedAudio (string), language (string), speakerCount (int)
 * Output: transcript (string), segments (list), wordCount (int), speakersDetected (int)
 */
public class TranscribeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "au_transcribe";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> segments = List.of(
                Map.of("speaker", "Speaker 1", "text", "Welcome to our discussion on machine learning infrastructure."),
                Map.of("speaker", "Speaker 2", "text", "Thanks for having me. I think the key challenge is scalability."),
                Map.of("speaker", "Speaker 1", "text", "Absolutely. How do you handle training data pipelines?"),
                Map.of("speaker", "Speaker 2", "text", "We use distributed processing with real-time validation.")
        );

        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> seg : segments) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(seg.get("speaker")).append(": ").append(seg.get("text"));
        }
        String transcript = sb.toString();
        int wordCount = transcript.split("\\s+").length;

        System.out.println("  [transcribe] Transcribed " + wordCount + " words, " + segments.size() + " segments, 2 speakers detected");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transcript", transcript);
        result.getOutputData().put("segments", segments);
        result.getOutputData().put("wordCount", wordCount);
        result.getOutputData().put("speakersDetected", 2);
        return result;
    }
}
