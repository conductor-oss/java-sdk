package priceoptimization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectMarketDataWorkerTest {

    private final CollectMarketDataWorker worker = new CollectMarketDataWorker();

    @Test
    void taskDefName() {
        assertEquals("prz_collect_market_data", worker.getTaskDefName());
    }

    @Test
    void returnsCompetitorPrices() {
        Task task = taskWith(Map.of("productId", "P-1", "category", "electronics"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Double> prices = (List<Double>) result.getOutputData().get("competitorPrices");
        assertNotNull(prices);
        assertEquals(4, prices.size());
    }

    @Test
    void returnsMarketTrend() {
        Task task = taskWith(Map.of("productId", "P-2", "category", "audio"));
        TaskResult result = worker.execute(task);

        assertEquals("upward", result.getOutputData().get("marketTrend"));
    }

    @Test
    void returnsAvgMarketPrice() {
        Task task = taskWith(Map.of("productId", "P-3", "category", "electronics"));
        TaskResult result = worker.execute(task);

        assertEquals(91.06, result.getOutputData().get("avgMarketPrice"));
    }

    @Test
    void returnsSupplyLevel() {
        Task task = taskWith(Map.of("productId", "P-4", "category", "electronics"));
        TaskResult result = worker.execute(task);

        assertEquals("moderate", result.getOutputData().get("supplyLevel"));
    }

    @Test
    void handlesNullProductId() {
        Map<String, Object> input = new HashMap<>();
        input.put("productId", null);
        input.put("category", "electronics");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullCategory() {
        Map<String, Object> input = new HashMap<>();
        input.put("productId", "P-5");
        input.put("category", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("competitorPrices"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
