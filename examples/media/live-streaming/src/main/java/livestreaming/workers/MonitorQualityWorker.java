package livestreaming.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Monitors stream quality metrics.
 * Input: streamId, playbackUrl, viewerCount
 * Output: peakViewers, avgBitrate, bufferRatio, duration, qualityScore
 */
public class MonitorQualityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lsm_monitor_quality";
    }

    @Override
    public TaskResult execute(Task task) {
        Object viewerCountObj = task.getInputData().get("viewerCount");
        String viewers = viewerCountObj != null ? viewerCountObj.toString() : "0";

        System.out.println("  [monitor] Monitoring stream quality — " + viewers + " viewers");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("peakViewers", 3200);
        result.getOutputData().put("avgBitrate", "4.2 Mbps");
        result.getOutputData().put("bufferRatio", 0.02);
        result.getOutputData().put("duration", 5500);
        result.getOutputData().put("qualityScore", 94);
        return result;
    }
}
