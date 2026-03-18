package functioncalling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM planning worker -- analyzes a user query and determines which function
 * to call by matching keywords in the query text.
 *
 * <p>Supported function mappings:
 * <ul>
 *   <li><b>get_weather</b> -- triggered by weather-related keywords
 *       (weather, temperature, forecast, rain, snow, etc.). Extracts the
 *       location from the query.</li>
 *   <li><b>get_stock_price</b> -- triggered by stock/price/ticker keywords.
 *       Extracts a ticker symbol or company name.</li>
 *   <li><b>calculate</b> -- triggered by math-related keywords or presence
 *       of arithmetic operators. Extracts the expression.</li>
 *   <li><b>get_time</b> -- triggered by time/date/clock keywords.
 *       Extracts an optional timezone.</li>
 * </ul>
 *
 * <p>Returns a structured {@code llmResponse} containing {@code reasoning}
 * and {@code functionCall} with {@code name} and {@code arguments}.
 */
public class LlmPlanWorker implements Worker {

    // Keyword patterns (case-insensitive)
    private static final Pattern WEATHER_PAT = Pattern.compile(
            "\\b(weather|temperature|forecast|rain|snow|sunny|cloudy|humid|wind|celsius|fahrenheit)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STOCK_PAT = Pattern.compile(
            "\\b(stock|price|ticker|share|market|nasdaq|nyse|s&p|dow)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MATH_PAT = Pattern.compile(
            "\\b(calculate|compute|math|sum|add|subtract|multiply|divide|square root|sqrt|power|factorial)\\b|[0-9]+\\s*[+\\-*/^]\\s*[0-9]+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PAT = Pattern.compile(
            "\\b(time|date|clock|hour|timezone|day of week|what day)\\b",
            Pattern.CASE_INSENSITIVE);

    // Ticker extraction -- look for uppercase sequences of 1-5 letters or known company names
    private static final Pattern TICKER_PAT = Pattern.compile("\\b([A-Z]{1,5})\\b");
    private static final Map<String, String> COMPANY_TO_TICKER = Map.of(
            "apple", "AAPL", "google", "GOOGL", "alphabet", "GOOGL",
            "amazon", "AMZN", "microsoft", "MSFT", "tesla", "TSLA",
            "meta", "META", "facebook", "META", "netflix", "NFLX",
            "nvidia", "NVDA"
    );

    @Override
    public String getTaskDefName() {
        return "fc_llm_plan";
    }

    @Override
    public TaskResult execute(Task task) {
        String userQuery = (String) task.getInputData().get("userQuery");
        if (userQuery == null || userQuery.isBlank()) {
            userQuery = "general query";
        }

        // functionDefinitions from input are acknowledged but not strictly required
        // for keyword-based planning
        Object functionDefinitions = task.getInputData().get("functionDefinitions");

        System.out.println("  [fc_llm_plan] Planning function call for query: " + userQuery);

        Map<String, Object> functionCall;
        String reasoning;

        if (WEATHER_PAT.matcher(userQuery).find()) {
            String location = extractLocation(userQuery);
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("location", location);
            args.put("units", userQuery.toLowerCase().contains("celsius") ? "celsius" : "fahrenheit");
            functionCall = buildFunctionCall("get_weather", args);
            reasoning = "The user is asking about weather conditions. "
                    + "I should call get_weather with location '" + location + "' to retrieve current weather data.";
        } else if (STOCK_PAT.matcher(userQuery).find()) {
            String ticker = extractTicker(userQuery);
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("ticker", ticker);
            args.put("includeChange", true);
            functionCall = buildFunctionCall("get_stock_price", args);
            reasoning = "The user is asking about stock/price information. "
                    + "I should call get_stock_price with ticker " + ticker + " to retrieve the latest price data.";
        } else if (MATH_PAT.matcher(userQuery).find()) {
            String expression = extractMathExpression(userQuery);
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("expression", expression);
            functionCall = buildFunctionCall("calculate", args);
            reasoning = "The user is asking for a mathematical calculation. "
                    + "I should call calculate with expression '" + expression + "'.";
        } else if (TIME_PAT.matcher(userQuery).find()) {
            String timezone = extractTimezone(userQuery);
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("timezone", timezone);
            functionCall = buildFunctionCall("get_time", args);
            reasoning = "The user is asking about the current time or date. "
                    + "I should call get_time to retrieve the current date/time information.";
        } else {
            // Fallback: try to make a best-effort determination
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("query", userQuery);
            functionCall = buildFunctionCall("general_query", args);
            reasoning = "The query does not clearly match a specific function. "
                    + "Routing to general_query for further processing.";
        }

        Map<String, Object> llmResponse = new LinkedHashMap<>();
        llmResponse.put("reasoning", reasoning);
        llmResponse.put("functionCall", functionCall);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("llmResponse", llmResponse);
        return result;
    }

    private Map<String, Object> buildFunctionCall(String name, Map<String, Object> arguments) {
        Map<String, Object> fc = new LinkedHashMap<>();
        fc.put("name", name);
        fc.put("arguments", arguments);
        return fc;
    }

    /**
     * Extracts a location from a weather query by removing weather-related
     * keywords and common filler words, leaving the location text.
     */
    private String extractLocation(String query) {
        String cleaned = query.replaceAll("(?i)\\b(what('s|\\s+is)|how('s|\\s+is)|the|weather|temperature|forecast|rain|snow|sunny|cloudy|humid|wind|celsius|fahrenheit|in|for|at|current|right now|today|tomorrow|like|outside|tell me|show me|get|check|please|can you)\\b", "")
                .replaceAll("[?.!,]", "")
                .trim()
                .replaceAll("\\s{2,}", " ")
                .trim();
        return cleaned.isEmpty() ? "Unknown" : cleaned;
    }

    /**
     * Extracts a stock ticker from the query, checking known company names
     * first, then looking for uppercase ticker-like patterns.
     */
    private String extractTicker(String query) {
        String lower = query.toLowerCase();
        for (Map.Entry<String, String> entry : COMPANY_TO_TICKER.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        Matcher m = TICKER_PAT.matcher(query);
        // Skip common English words that happen to be uppercase
        while (m.find()) {
            String candidate = m.group(1);
            if (!candidate.matches("(?i)GET|THE|FOR|AND|HOW|WHAT|SHOW|TELL|CAN|YOU|IS|OF|A|I|IT|MY")) {
                return candidate;
            }
        }
        return "UNKNOWN";
    }

    /**
     * Extracts a math expression from the query text.
     */
    private String extractMathExpression(String query) {
        // Look for patterns like "2 + 3 * 4" or "123 / 45"
        Matcher m = Pattern.compile("[0-9]+(?:\\s*[+\\-*/^]\\s*[0-9]+)+").matcher(query);
        if (m.find()) {
            return m.group().replaceAll("\\s+", "");
        }
        // Fall back: extract all numbers
        String cleaned = query.replaceAll("(?i)\\b(calculate|compute|what is|what's|the result of|please|can you)\\b", "")
                .trim();
        return cleaned.isEmpty() ? "0" : cleaned;
    }

    /**
     * Extracts a timezone identifier from the query text.
     */
    private String extractTimezone(String query) {
        Matcher m = Pattern.compile("(?i)\\b(UTC|GMT|EST|CST|MST|PST|[A-Z]{2,4}/[A-Za-z_]+)\\b").matcher(query);
        if (m.find()) {
            return m.group(1);
        }
        // Look for city-based timezone references
        String lower = query.toLowerCase();
        if (lower.contains("new york") || lower.contains("eastern")) return "America/New_York";
        if (lower.contains("los angeles") || lower.contains("pacific")) return "America/Los_Angeles";
        if (lower.contains("chicago") || lower.contains("central")) return "America/Chicago";
        if (lower.contains("london")) return "Europe/London";
        if (lower.contains("tokyo")) return "Asia/Tokyo";
        return "UTC";
    }
}
