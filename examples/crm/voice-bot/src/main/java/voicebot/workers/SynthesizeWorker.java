package voicebot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class SynthesizeWorker implements Worker {
    @Override public String getTaskDefName() { return "vb_synthesize"; }
    @Override public TaskResult execute(Task task) {
        String audioUrl = "https://tts.example.com/audio/" + System.currentTimeMillis() + ".mp3";
        System.out.println("  [synthesize] Speech synthesized -> " + audioUrl);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("audioUrl", audioUrl);
        result.getOutputData().put("durationSec", 5.1);
        result.getOutputData().put("format", "mp3");
        result.getOutputData().put("voice", "en-US-Neural2-F");
        return result;
    }
}
