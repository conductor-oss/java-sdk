package tooluseparallel.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CallStocksWorkerTest {

    private final CallStocksWorker worker = new CallStocksWorker();

    @Test
    void taskDefName() {
        assertEquals("tp_call_stocks", worker.getTaskDefName());
    }

    @Test
    void returnsQuotesForRequestedTickers() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("tickers", List.of("AAPL", "GOOGL", "MSFT", "AMZN")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> quotes = (List<Map<String, Object>>) result.getOutputData().get("quotes");
        assertNotNull(quotes);
        assertEquals(4, quotes.size(), "Should return one quote per requested ticker");
    }

    @Test
    void quotesContainTickerAndPrice() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("tickers", List.of("AAPL")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> quotes = (List<Map<String, Object>>) result.getOutputData().get("quotes");
        Map<String, Object> first = quotes.get(0);
        assertEquals("AAPL", first.get("ticker"));
        // If no error, check price fields
        if (!first.containsKey("error")) {
            assertNotNull(first.get("price"));
            assertNotNull(first.get("change"));
            assertNotNull(first.get("changePercent"));
        }
    }

    @Test
    void returnsMarketSentiment() {
        Task task = taskWith(Map.of("tickers", List.of("MSFT")));
        TaskResult result = worker.execute(task);

        String sentiment = (String) result.getOutputData().get("marketSentiment");
        assertNotNull(sentiment);
        assertTrue(List.of("bullish", "bearish", "neutral").contains(sentiment),
                "Sentiment should be bullish, bearish, or neutral, got: " + sentiment);
    }

    @Test
    void returnsYahooFinanceSource() {
        Task task = taskWith(Map.of("tickers", List.of("AMZN")));
        TaskResult result = worker.execute(task);

        assertEquals("yahoo_finance", result.getOutputData().get("source"));
    }

    @Test
    void handlesNullTickers() {
        Map<String, Object> input = new HashMap<>();
        input.put("tickers", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("quotes"));
    }

    @Test
    void handlesMissingTickers() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("quotes"));
    }

    @Test
    void quotesHaveNumericPricesWhenApiSucceeds() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("tickers", List.of("AAPL", "GOOGL")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> quotes = (List<Map<String, Object>>) result.getOutputData().get("quotes");
        for (Map<String, Object> quote : quotes) {
            if (!quote.containsKey("error")) {
                assertInstanceOf(Number.class, quote.get("price"),
                        "Price should be numeric for " + quote.get("ticker"));
                assertInstanceOf(Number.class, quote.get("change"),
                        "Change should be numeric for " + quote.get("ticker"));
            }
        }
    }

    @Test
    void allRequestedTickersPresent() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("tickers", List.of("AAPL", "GOOGL", "MSFT", "AMZN")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> quotes = (List<Map<String, Object>>) result.getOutputData().get("quotes");
        List<String> quoteTickers = quotes.stream()
                .map(q -> (String) q.get("ticker"))
                .toList();
        assertTrue(quoteTickers.contains("AAPL"));
        assertTrue(quoteTickers.contains("GOOGL"));
        assertTrue(quoteTickers.contains("MSFT"));
        assertTrue(quoteTickers.contains("AMZN"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }

    /**
     * Checks whether a network connection to Yahoo Finance is available.
     */
    private static boolean isNetworkAvailable() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://query2.finance.yahoo.com/v8/finance/chart/AAPL?range=1d&interval=1d"))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
