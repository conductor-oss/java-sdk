package tooluseparallel.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Combine results worker — merges the parallel weather, news, and stocks data
 * into a unified morning briefing object.
 */
public class CombineResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tp_combine_results";
    }

    @Override
    public TaskResult execute(Task task) {
        Object weather = task.getInputData().get("weather");
        Object news = task.getInputData().get("news");
        Object stocks = task.getInputData().get("stocks");

        System.out.println("  [tp_combine_results] Combining weather, news, and stocks into briefing");

        Map<String, Object> briefing = Map.of(
                "weather", weather != null ? weather : Map.of(),
                "news", news != null ? news : List.of(),
                "stocks", stocks != null ? stocks : List.of(),
                "summary", "Good morning! Current conditions show Morning Fog at 58F with a high of 68F expected. "
                        + "In the news: AI breakthroughs and strong tech earnings dominate headlines. "
                        + "Markets are bullish with AAPL, GOOGL, and AMZN all up over 1%."
        );

        List<String> toolsUsed = List.of("weather_api", "news_api", "stock_api");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("briefing", briefing);
        result.getOutputData().put("toolsUsed", toolsUsed);
        result.getOutputData().put("generatedAt", "2026-03-08T08:00:00Z");
        return result;
    }
}
