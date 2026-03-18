package functioncalling.workers;

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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes the specified function with the given arguments.
 *
 * <p>Supported functions:
 * <ul>
 *   <li><b>get_weather</b> — Fetches real weather data from the Open-Meteo API
 *       (geocoding-api.open-meteo.com for lat/lon, api.open-meteo.com for forecast).
 *       No API key required.</li>
 *   <li><b>calculate</b> — Evaluates basic arithmetic expressions (+, -, *, /)</li>
 *   <li><b>get_time</b> — Returns the current UTC time</li>
 *   <li><b>get_stock_price</b> — Fetches real stock price data from Yahoo Finance
 *       (query2.finance.yahoo.com). No API key required.</li>
 * </ul>
 *
 * <p>All HTTP calls use {@link java.net.http.HttpClient} with a 10-second timeout.
 * Network errors are returned as error maps rather than crashing the worker.
 */
public class ExecuteFunctionWorker implements Worker {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getTaskDefName() {
        return "fc_execute_function";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String functionName = (String) task.getInputData().get("functionName");
        Map<String, Object> arguments = (Map<String, Object>) task.getInputData().get("arguments");

        if (functionName == null || functionName.isBlank()) {
            functionName = "unknown";
        }
        if (arguments == null) {
            arguments = Map.of();
        }

        System.out.println("  [fc_execute_function] Executing function: " + functionName + " with args: " + arguments);

        TaskResult result = new TaskResult(task);

        try {
            Map<String, Object> functionResult = switch (functionName) {
                case "get_weather" -> executeGetWeather(arguments);
                case "calculate" -> executeCalculate(arguments);
                case "get_time" -> executeGetTime(arguments);
                case "get_stock_price" -> executeGetStockPrice(arguments);
                default -> {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("error", "Function not found: " + functionName);
                    result.getOutputData().put("result", err);
                    result.getOutputData().put("executionStatus", "error");
                    result.setStatus(TaskResult.Status.COMPLETED);
                    yield null;
                }
            };

            if (functionResult != null) {
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("result", functionResult);
                result.getOutputData().put("executionStatus", "success");
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("error", "Function execution failed: " + e.getMessage());
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("result", errorResult);
            result.getOutputData().put("executionStatus", "error");
        }

        return result;
    }

    /**
     * Fetches real weather data from the Open-Meteo API.
     *
     * <ol>
     *   <li>Geocodes the city via GET https://geocoding-api.open-meteo.com/v1/search?name={city}&amp;count=1</li>
     *   <li>Fetches current weather via GET https://api.open-meteo.com/v1/forecast?latitude={lat}&amp;longitude={lon}&amp;current=temperature_2m,wind_speed_10m,weather_code</li>
     * </ol>
     *
     * @param args must contain "location" (String); optional "unit" ("celsius" or "fahrenheit")
     * @return map with location, temperature, unit, condition, windSpeed, weatherCode, timestamp
     */
    private Map<String, Object> executeGetWeather(Map<String, Object> args) {
        String location = (String) args.getOrDefault("location", "New York");
        String unit = (String) args.getOrDefault("unit", "celsius");

        try {
            // Step 1: Geocode the city
            String encodedCity = URLEncoder.encode(location, StandardCharsets.UTF_8);
            String geocodeUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCity + "&count=1";

            HttpRequest geocodeReq = HttpRequest.newBuilder()
                    .uri(URI.create(geocodeUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> geocodeResp = HTTP_CLIENT.send(geocodeReq, HttpResponse.BodyHandlers.ofString());

            if (geocodeResp.statusCode() != 200) {
                return errorMap("Geocoding API returned status " + geocodeResp.statusCode());
            }

            String geoBody = geocodeResp.body();

            // Minimal JSON parsing without external library
            if (!geoBody.contains("\"results\"")) {
                return errorMap("Location not found: " + location);
            }

            double latitude = extractDouble(geoBody, "\"latitude\"");
            double longitude = extractDouble(geoBody, "\"longitude\"");

            // Step 2: Fetch weather
            String tempUnit = "fahrenheit".equalsIgnoreCase(unit) ? "&temperature_unit=fahrenheit" : "";
            String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&current=temperature_2m,wind_speed_10m,weather_code"
                    + tempUnit;

            HttpRequest weatherReq = HttpRequest.newBuilder()
                    .uri(URI.create(weatherUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> weatherResp = HTTP_CLIENT.send(weatherReq, HttpResponse.BodyHandlers.ofString());

            if (weatherResp.statusCode() != 200) {
                return errorMap("Weather API returned status " + weatherResp.statusCode());
            }

            String wxBody = weatherResp.body();

            double temperature = extractDouble(wxBody, "\"temperature_2m\"");
            double windSpeed = extractDouble(wxBody, "\"wind_speed_10m\"");
            int weatherCode = (int) extractDouble(wxBody, "\"weather_code\"");

            String condition = weatherCodeToCondition(weatherCode);
            String unitLabel = "fahrenheit".equalsIgnoreCase(unit) ? "F" : "C";

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("location", location);
            result.put("temperature", Math.round(temperature * 10.0) / 10.0);
            result.put("unit", unitLabel);
            result.put("condition", condition);
            result.put("windSpeed", Math.round(windSpeed * 10.0) / 10.0);
            result.put("weatherCode", weatherCode);
            result.put("timestamp", Instant.now().toString());
            return result;

        } catch (Exception e) {
            return errorMap("Weather lookup failed: " + e.getMessage());
        }
    }

    /**
     * Evaluates a basic arithmetic expression. Supports +, -, *, / with two operands,
     * or parses a simple "expression" string.
     */
    private Map<String, Object> executeCalculate(Map<String, Object> args) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Try expression string first
        Object exprObj = args.get("expression");
        if (exprObj instanceof String) {
            String expr = ((String) exprObj).trim();
            double value = evaluateExpression(expr);
            result.put("expression", expr);
            result.put("result", value);
            return result;
        }

        // Fall back to operand-based calculation
        double a = toDouble(args.getOrDefault("a", args.getOrDefault("operand1", 0)));
        double b = toDouble(args.getOrDefault("b", args.getOrDefault("operand2", 0)));
        String op = (String) args.getOrDefault("operation", args.getOrDefault("operator", "+"));

        double value = switch (op) {
            case "+", "add" -> a + b;
            case "-", "subtract" -> a - b;
            case "*", "multiply" -> a * b;
            case "/", "divide" -> {
                if (b == 0) throw new ArithmeticException("Division by zero");
                yield a / b;
            }
            case "^", "power" -> Math.pow(a, b);
            case "%", "mod" -> a % b;
            default -> throw new IllegalArgumentException("Unsupported operation: " + op);
        };

        result.put("a", a);
        result.put("b", b);
        result.put("operation", op);
        result.put("result", value);
        return result;
    }

    /**
     * Returns the current UTC time.
     */
    private Map<String, Object> executeGetTime(Map<String, Object> args) {
        String timezone = (String) args.getOrDefault("timezone", "UTC");
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezone);
        } catch (Exception e) {
            zoneId = ZoneId.of("UTC");
            timezone = "UTC";
        }

        Instant now = Instant.now();
        String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(zoneId)
                .format(now);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timezone", timezone);
        result.put("datetime", formatted);
        result.put("epochMillis", now.toEpochMilli());
        return result;
    }

    /**
     * Fetches real stock price data from Yahoo Finance.
     *
     * <p>Calls GET https://query2.finance.yahoo.com/v8/finance/chart/{ticker}?range=1d&amp;interval=1d
     * and parses the regularMarketPrice, chartPreviousClose, and volume from the response.
     *
     * @param args must contain "ticker" (String)
     * @return map with ticker, price, currency, change, changePercent, volume, timestamp
     */
    private Map<String, Object> executeGetStockPrice(Map<String, Object> args) {
        String ticker = (String) args.getOrDefault("ticker", "UNKNOWN");

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
                return errorMap("Yahoo Finance API returned status " + response.statusCode() + " for ticker " + ticker);
            }

            String body = response.body();

            if (body.contains("\"error\"") && !body.contains("\"error\":null")) {
                return errorMap("Ticker not found: " + ticker);
            }

            double price = extractDouble(body, "\"regularMarketPrice\"");
            double previousClose = extractDouble(body, "\"chartPreviousClose\"");
            double change = Math.round((price - previousClose) * 100.0) / 100.0;
            double changePercent = previousClose > 0
                    ? Math.round((change / previousClose) * 10000.0) / 100.0
                    : 0;

            // Extract currency
            String currency = extractString(body, "\"currency\"");
            if (currency == null || currency.isEmpty()) {
                currency = "USD";
            }

            // Extract volume
            long volume = 0;
            try {
                volume = (long) extractDouble(body, "\"regularMarketVolume\"");
            } catch (Exception ignored) {
                // volume is optional
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ticker", ticker);
            result.put("price", price);
            result.put("currency", currency);
            result.put("change", change);
            result.put("changePercent", changePercent);
            result.put("volume", volume);
            result.put("timestamp", Instant.now().toString());
            return result;

        } catch (Exception e) {
            return errorMap("Stock lookup failed for " + ticker + ": " + e.getMessage());
        }
    }

    // --- Utility methods ---

    /**
     * Converts a WMO weather code to a human-readable condition string.
     */
    private String weatherCodeToCondition(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snowfall";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown (" + code + ")";
        };
    }

    /**
     * Extracts the first double value after the given JSON key from a raw JSON string.
     * This is a minimal parser that avoids depending on a JSON library.
     */
    private double extractDouble(String json, String key) {
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) throw new IllegalArgumentException("Key not found in response: " + key);
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

    /**
     * Extracts the first string value after the given JSON key from a raw JSON string.
     */
    private String extractString(String json, String key) {
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx < 0) return null;
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart < 0) return null;
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return null;
        return json.substring(quoteStart + 1, quoteEnd);
    }

    /**
     * Creates an error result map with the given message.
     */
    private Map<String, Object> errorMap(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("error", message);
        return result;
    }

    /**
     * Simple expression evaluator for basic arithmetic: "2 + 3", "10 * 5", etc.
     */
    private double evaluateExpression(String expr) {
        expr = expr.trim();
        // Try to find an operator
        for (char op : new char[]{'+', '-', '*', '/', '^', '%'}) {
            // Search from right for + and -, from left for others (basic precedence)
            int idx = (op == '+' || op == '-')
                    ? expr.lastIndexOf(op) : expr.indexOf(op);
            // Don't match leading minus sign
            if (idx > 0) {
                String left = expr.substring(0, idx).trim();
                String right = expr.substring(idx + 1).trim();
                if (!left.isEmpty() && !right.isEmpty()) {
                    double a = Double.parseDouble(left);
                    double b = Double.parseDouble(right);
                    return switch (op) {
                        case '+' -> a + b;
                        case '-' -> a - b;
                        case '*' -> a * b;
                        case '/' -> {
                            if (b == 0) throw new ArithmeticException("Division by zero");
                            yield a / b;
                        }
                        case '^' -> Math.pow(a, b);
                        case '%' -> a % b;
                        default -> throw new IllegalArgumentException("Unknown operator: " + op);
                    };
                }
            }
        }
        // No operator found — try to parse as a single number
        return Double.parseDouble(expr);
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) return Double.parseDouble((String) value);
        return 0.0;
    }
}
