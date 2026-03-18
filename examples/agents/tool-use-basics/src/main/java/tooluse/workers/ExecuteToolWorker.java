package tooluse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes the selected tool and returns its output.
 *
 * <p>Supported tools:
 * <ul>
 *   <li><b>weather_api</b> -- calls the Open-Meteo geocoding + forecast APIs to
 *       return real current weather for the requested location.</li>
 *   <li><b>web_search</b> -- calls the Wikipedia opensearch API for real search
 *       results matching the query.</li>
 *   <li><b>calculator</b> -- evaluates a mathematical expression and returns
 *       the numeric result.</li>
 * </ul>
 *
 * <p>Network errors are caught and returned as an {@code error} field inside
 * the tool output rather than propagating as exceptions.
 */
public class ExecuteToolWorker implements Worker {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getTaskDefName() {
        return "tu_execute_tool";
    }

    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> toolArgs = (Map<String, Object>) task.getInputData().get("toolArgs");

        System.out.println("  [tu_execute_tool] Executing tool: " + toolName);

        long start = System.currentTimeMillis();
        Map<String, Object> toolOutput;

        switch (toolName) {
            case "weather_api":
                toolOutput = executeWeather(toolArgs);
                break;
            case "calculator":
                toolOutput = executeCalculator(toolArgs);
                break;
            case "web_search":
                toolOutput = executeWebSearch(toolArgs);
                break;
            default:
                toolOutput = Map.of(
                        "message", "No handler available for tool: " + toolName,
                        "status", "unsupported"
                );
                break;
        }

        long elapsed = System.currentTimeMillis() - start;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("toolOutput", toolOutput);
        result.getOutputData().put("executionTimeMs", elapsed);
        result.getOutputData().put("success", !toolOutput.containsKey("error"));
        return result;
    }

    // ---- weather via Open-Meteo ------------------------------------------------

    private Map<String, Object> executeWeather(Map<String, Object> toolArgs) {
        String location = toolArgs != null && toolArgs.get("location") != null
                ? (String) toolArgs.get("location") : "Unknown";
        String units = toolArgs != null && toolArgs.get("units") != null
                ? (String) toolArgs.get("units") : "fahrenheit";

        try {
            // Step 1: geocode the location
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                    + URLEncoder.encode(location, StandardCharsets.UTF_8)
                    + "&count=1&language=en&format=json";

            HttpRequest geoReq = HttpRequest.newBuilder()
                    .uri(URI.create(geoUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<String> geoResp = HTTP.send(geoReq, HttpResponse.BodyHandlers.ofString());
            JsonNode geoJson = MAPPER.readTree(geoResp.body());

            JsonNode results = geoJson.get("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                return Map.of("error", "Location not found: " + location, "location", location);
            }

            double lat = results.get(0).get("latitude").asDouble();
            double lon = results.get(0).get("longitude").asDouble();
            String resolvedName = results.get(0).has("name") ? results.get(0).get("name").asText() : location;

            // Step 2: fetch weather
            String tempUnit = "celsius".equalsIgnoreCase(units) ? "celsius" : "fahrenheit";
            String windUnit = "celsius".equalsIgnoreCase(units) ? "kmh" : "mph";
            String weatherUrl = "https://api.open-meteo.com/v1/forecast?"
                    + "latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,weather_code"
                    + "&daily=temperature_2m_max,temperature_2m_min,weather_code"
                    + "&temperature_unit=" + tempUnit
                    + "&wind_speed_unit=" + windUnit
                    + "&forecast_days=3&timezone=auto";

            HttpRequest wxReq = HttpRequest.newBuilder()
                    .uri(URI.create(weatherUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<String> wxResp = HTTP.send(wxReq, HttpResponse.BodyHandlers.ofString());
            JsonNode wx = MAPPER.readTree(wxResp.body());

            JsonNode current = wx.get("current");
            double temperature = current.get("temperature_2m").asDouble();
            int humidity = current.get("relative_humidity_2m").asInt();
            double windSpeed = current.get("wind_speed_10m").asDouble();
            int windDir = current.get("wind_direction_10m").asInt();
            int weatherCode = current.get("weather_code").asInt();
            String condition = weatherCodeToCondition(weatherCode);

            // Build forecast for the next two days (indices 1 and 2)
            JsonNode daily = wx.get("daily");
            List<Map<String, Object>> forecast = new ArrayList<>();
            JsonNode dates = daily.get("time");
            JsonNode highs = daily.get("temperature_2m_max");
            JsonNode lows = daily.get("temperature_2m_min");
            JsonNode codes = daily.get("weather_code");

            for (int i = 1; i < Math.min(3, dates.size()); i++) {
                Map<String, Object> day = new LinkedHashMap<>();
                day.put("day", i == 1 ? "Tomorrow" : "Day After");
                day.put("high", highs.get(i).asDouble());
                day.put("low", lows.get(i).asDouble());
                day.put("condition", weatherCodeToCondition(codes.get(i).asInt()));
                forecast.add(day);
            }

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("location", resolvedName);
            out.put("temperature", temperature);
            out.put("units", units);
            out.put("condition", condition);
            out.put("humidity", humidity);
            out.put("windSpeed", windSpeed);
            out.put("windDirection", degreesToCardinal(windDir));
            out.put("forecast", forecast);
            return out;

        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Weather API call failed: " + e.getMessage());
            err.put("location", location);
            return err;
        }
    }

    // ---- calculator ------------------------------------------------------------

    private Map<String, Object> executeCalculator(Map<String, Object> toolArgs) {
        String expression = toolArgs != null && toolArgs.get("expression") != null
                ? String.valueOf(toolArgs.get("expression")) : "0";
        int precision = 2;
        if (toolArgs != null && toolArgs.get("precision") != null) {
            precision = ((Number) toolArgs.get("precision")).intValue();
        }

        try {
            double value = evaluateExpression(expression);
            double factor = Math.pow(10, precision);
            double rounded = Math.round(value * factor) / factor;

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("expression", expression);
            out.put("result", rounded);
            out.put("precision", precision);
            return out;
        } catch (Exception e) {
            return Map.of(
                    "expression", expression,
                    "error", "Failed to evaluate expression: " + e.getMessage(),
                    "result", 0
            );
        }
    }

    // ---- web search via Wikipedia opensearch -----------------------------------

    private Map<String, Object> executeWebSearch(Map<String, Object> toolArgs) {
        String query = toolArgs != null && toolArgs.get("query") != null
                ? (String) toolArgs.get("query") : "";
        int maxResults = 5;
        if (toolArgs != null && toolArgs.get("maxResults") != null) {
            maxResults = ((Number) toolArgs.get("maxResults")).intValue();
        }

        try {
            String url = "https://en.wikipedia.org/w/api.php?action=opensearch&search="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&limit=" + maxResults
                    + "&namespace=0&format=json";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = MAPPER.readTree(resp.body());

            // opensearch returns: [query, [titles...], [descriptions...], [urls...]]
            JsonNode titles = json.get(1);
            JsonNode urls = json.get(3);

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (int i = 0; i < titles.size(); i++) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("title", titles.get(i).asText());
                entry.put("url", urls.get(i).asText());
                resultList.add(entry);
            }

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("query", query);
            out.put("results", resultList);
            out.put("totalResults", resultList.size());
            return out;

        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("query", query);
            err.put("error", "Web search failed: " + e.getMessage());
            err.put("results", List.of());
            err.put("totalResults", 0);
            return err;
        }
    }

    // ---- helpers ---------------------------------------------------------------

    /**
     * Evaluates a simple mathematical expression supporting +, -, *, /, ^,
     * parentheses, and unary minus via a recursive-descent parser.
     */
    static double evaluateExpression(String expr) {
        String s = expr.replaceAll("\\s+", "");
        double[] result = new double[1];
        int[] pos = {0};
        result[0] = parseExpr(s, pos);
        return result[0];
    }

    private static double parseExpr(String s, int[] pos) {
        double val = parseTerm(s, pos);
        while (pos[0] < s.length() && (s.charAt(pos[0]) == '+' || s.charAt(pos[0]) == '-')) {
            char op = s.charAt(pos[0]++);
            double right = parseTerm(s, pos);
            val = op == '+' ? val + right : val - right;
        }
        return val;
    }

    private static double parseTerm(String s, int[] pos) {
        double val = parsePower(s, pos);
        while (pos[0] < s.length() && (s.charAt(pos[0]) == '*' || s.charAt(pos[0]) == '/')) {
            char op = s.charAt(pos[0]++);
            double right = parsePower(s, pos);
            val = op == '*' ? val * right : val / right;
        }
        return val;
    }

    private static double parsePower(String s, int[] pos) {
        double val = parseUnary(s, pos);
        if (pos[0] < s.length() && s.charAt(pos[0]) == '^') {
            pos[0]++;
            double right = parsePower(s, pos); // right-associative
            val = Math.pow(val, right);
        }
        return val;
    }

    private static double parseUnary(String s, int[] pos) {
        if (pos[0] < s.length() && s.charAt(pos[0]) == '-') {
            pos[0]++;
            return -parseUnary(s, pos);
        }
        return parseAtom(s, pos);
    }

    private static double parseAtom(String s, int[] pos) {
        if (pos[0] < s.length() && s.charAt(pos[0]) == '(') {
            pos[0]++;
            double val = parseExpr(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ')') pos[0]++;
            return val;
        }
        int start = pos[0];
        while (pos[0] < s.length() && (Character.isDigit(s.charAt(pos[0])) || s.charAt(pos[0]) == '.')) {
            pos[0]++;
        }
        return Double.parseDouble(s.substring(start, pos[0]));
    }

    /** Maps WMO weather codes to human-readable condition strings. */
    private static String weatherCodeToCondition(int code) {
        return switch (code) {
            case 0 -> "Clear Sky";
            case 1 -> "Mainly Clear";
            case 2 -> "Partly Cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing Rain";
            case 71, 73, 75 -> "Snowfall";
            case 77 -> "Snow Grains";
            case 80, 81, 82 -> "Rain Showers";
            case 85, 86 -> "Snow Showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with Hail";
            default -> "Unknown (" + code + ")";
        };
    }

    /** Converts wind direction in degrees to a cardinal direction string. */
    private static String degreesToCardinal(int degrees) {
        String[] dirs = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int idx = (int) Math.round(degrees / 22.5) % 16;
        return dirs[idx];
    }
}
