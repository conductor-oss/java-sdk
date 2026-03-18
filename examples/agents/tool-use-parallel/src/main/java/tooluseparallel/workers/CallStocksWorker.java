package tooluseparallel.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Call stocks worker — fetches real stock market data from Yahoo Finance.
 *
 * <p>For each ticker in the input list, calls:
 * GET https://query2.finance.yahoo.com/v8/finance/chart/{ticker}?range=1d&amp;interval=1d
 *
 * <p>Parses regularMarketPrice, chartPreviousClose from chart.result[0].meta and computes
 * change and changePercent. Errors are handled per-ticker so that one failure does not
 * block the others.
 *
 * <p>No API key is required. Uses {@link java.net.http.HttpClient} with a 10-second timeout.
 */
public class CallStocksWorker implements Worker {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getTaskDefName() {
        return "tp_call_stocks";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> tickers = (List<String>) task.getInputData().get("tickers");
        if (tickers == null || tickers.isEmpty()) {
            tickers = List.of("SPY");
        }

        System.out.println("  [tp_call_stocks] Fetching stock quotes for: " + tickers);

        List<Map<String, Object>> quotes = new ArrayList<>();
        int gainers = 0;
        int losers = 0;

        for (String ticker : tickers) {
            Map<String, Object> quote = fetchQuote(ticker);
            quotes.add(quote);

            // Track sentiment
            if (!quote.containsKey("error")) {
                Object changeObj = quote.get("change");
                if (changeObj instanceof Number) {
                    double change = ((Number) changeObj).doubleValue();
                    if (change > 0) gainers++;
                    else if (change < 0) losers++;
                }
            }
        }

        String sentiment;
        if (gainers > losers) sentiment = "bullish";
        else if (losers > gainers) sentiment = "bearish";
        else sentiment = "neutral";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("quotes", quotes);
        result.getOutputData().put("marketSentiment", sentiment);
        result.getOutputData().put("source", "yahoo_finance");
        return result;
    }

    private Map<String, Object> fetchQuote(String ticker) {
        Map<String, Object> quote = new LinkedHashMap<>();
        quote.put("ticker", ticker);

        try {
            String url = "https://query2.finance.yahoo.com/v8/finance/chart/"
                    + URLEncoder.encode(ticker, StandardCharsets.UTF_8)
                    + "?range=1d&interval=1d";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                quote.put("error", "HTTP " + response.statusCode());
                return quote;
            }

            String body = response.body();

            if (body.contains("\"error\"") && !body.contains("\"error\":null")) {
                quote.put("error", "Ticker not found: " + ticker);
                return quote;
            }

            double price = extractDouble(body, "\"regularMarketPrice\"");
            double previousClose = extractDouble(body, "\"chartPreviousClose\"");
            double change = Math.round((price - previousClose) * 100.0) / 100.0;
            double changePercent = previousClose > 0
                    ? Math.round((change / previousClose) * 10000.0) / 100.0
                    : 0;

            quote.put("price", price);
            quote.put("change", change);
            quote.put("changePercent", changePercent);

        } catch (Exception e) {
            quote.put("error", "Failed to fetch " + ticker + ": " + e.getMessage());
        }

        return quote;
    }

    private double extractDouble(String json, String key) {
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) throw new IllegalArgumentException("Key not found: " + key);
        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx < 0) throw new IllegalArgumentException("Malformed JSON after key: " + key);
        int start = colonIdx + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\n' || json.charAt(start) == '\r')) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-' || json.charAt(end) == 'E' || json.charAt(end) == 'e' || json.charAt(end) == '+')) {
            end++;
        }
        return Double.parseDouble(json.substring(start, end));
    }
}
