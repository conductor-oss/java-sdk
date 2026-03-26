package audiotranscription.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Preprocesses audio: normalization, noise reduction.
 * Input: audioUrl (string), language (string)
 * Output: processedAudio (string), duration (string), sampleRate (int), channels (int)
 */
public class PreprocessAudioWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "au_preprocess_audio";
    }

    @Override
    public TaskResult execute(Task task) {
        String url = (String) task.getInputData().getOrDefault("audioUrl", "unknown.wav");

        System.out.println("  [preprocess] Audio: " + url + " — 44.1kHz stereo, normalized, noise reduced");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedAudio", "processed_audio_data");
        result.getOutputData().put("duration", "8m45s");
        result.getOutputData().put("sampleRate", 44100);
        result.getOutputData().put("channels", 2);
        return result;
    }
}
