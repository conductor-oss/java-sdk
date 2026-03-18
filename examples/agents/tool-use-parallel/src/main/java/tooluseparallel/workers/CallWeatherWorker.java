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
import java.time.Instant;
import java.util.*;

/**
 * Call weather worker — fetches real weather data from the Open-Meteo API.
 *
 * <p>For a given location string (e.g. "New York, NY"):
 * <ol>
 *   <li>Geocodes the location via GET https://geocoding-api.open-meteo.com/v1/search?name={city}&amp;count=1</li>
 *   <li>Fetches current weather + hourly forecast via GET https://api.open-meteo.com/v1/forecast
 *       with parameters for current conditions (temperature, wind speed, weather code)
 *       and hourly forecast for the next 4 hours.</li>
 * </ol>
 *
 * <p>No API key is required. Uses {@link java.net.http.HttpClient} with a 10-second timeout.
 * On network failure, returns an error in the weatherData output rather than crashing.
 */
public class CallWeatherWorker implements Worker {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getTaskDefName() {
        return "tp_call_weather";
    }

    @Override
    public TaskResult execute(Task task) {
        String location = (String) task.getInputData().get("location");
        if (location == null || location.isBlank()) {
            location = "New York, NY";
        }

        System.out.println("  [tp_call_weather] Fetching weather for: " + location);

        TaskResult result = new TaskResult(task);

        try {
            Map<String, Object> weatherData = fetchWeatherData(location);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("weatherData", weatherData);
            result.getOutputData().put("source", "open-meteo.com");
        } catch (Exception e) {
            System.out.println("  [tp_call_weather] Error: " + e.getMessage());
            Map<String, Object> errorData = new LinkedHashMap<>();
            errorData.put("location", location);
            errorData.put("error", e.getMessage());
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("weatherData", errorData);
            result.getOutputData().put("source", "open-meteo.com");
        }

        return result;
    }

    private Map<String, Object> fetchWeatherData(String location) throws Exception {
        // Step 1: Geocode
        String encodedCity = URLEncoder.encode(location, StandardCharsets.UTF_8);
        String geocodeUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCity + "&count=1";

        HttpRequest geocodeReq = HttpRequest.newBuilder()
                .uri(URI.create(geocodeUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> geocodeResp = HTTP_CLIENT.send(geocodeReq, HttpResponse.BodyHandlers.ofString());
        if (geocodeResp.statusCode() != 200) {
            throw new RuntimeException("Geocoding API returned status " + geocodeResp.statusCode());
        }

        String geoBody = geocodeResp.body();
        if (!geoBody.contains("\"results\"")) {
            throw new RuntimeException("Location not found: " + location);
        }

        double latitude = extractDouble(geoBody, "\"latitude\"");
        double longitude = extractDouble(geoBody, "\"longitude\"");

        // Step 2: Fetch current + hourly weather
        String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,wind_speed_10m,weather_code,relative_humidity_2m"
                + "&hourly=temperature_2m,weather_code"
                + "&forecast_hours=4"
                + "&temperature_unit=fahrenheit";

        HttpRequest weatherReq = HttpRequest.newBuilder()
                .uri(URI.create(weatherUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> weatherResp = HTTP_CLIENT.send(weatherReq, HttpResponse.BodyHandlers.ofString());
        if (weatherResp.statusCode() != 200) {
            throw new RuntimeException("Weather API returned status " + weatherResp.statusCode());
        }

        String wxBody = weatherResp.body();

        // Parse current conditions from the "current" block
        String currentBlock = extractBlock(wxBody, "\"current\"");
        double currentTemp = extractDouble(currentBlock, "\"temperature_2m\"");
        double windSpeed = extractDouble(currentBlock, "\"wind_speed_10m\"");
        int weatherCode = (int) extractDouble(currentBlock, "\"weather_code\"");
        int humidity;
        try {
            humidity = (int) extractDouble(currentBlock, "\"relative_humidity_2m\"");
        } catch (Exception e) {
            humidity = -1;
        }

        String condition = weatherCodeToCondition(weatherCode);

        Map<String, Object> current = new LinkedHashMap<>();
        current.put("temp", Math.round(currentTemp));
        current.put("condition", condition);
        if (humidity >= 0) {
            current.put("humidity", humidity);
        }
        current.put("windSpeed", Math.round(windSpeed * 10.0) / 10.0);

        // Parse hourly forecast from the "hourly" block
        List<Map<String, Object>> hourly = parseHourlyForecast(wxBody);

        // Compute high/low from hourly data + current
        int high = (int) Math.round(currentTemp);
        int low = (int) Math.round(currentTemp);
        for (Map<String, Object> h : hourly) {
            Object tempObj = h.get("temp");
            if (tempObj instanceof Number) {
                int t = ((Number) tempObj).intValue();
                if (t > high) high = t;
                if (t < low) low = t;
            }
        }

        Map<String, Object> weatherData = new LinkedHashMap<>();
        weatherData.put("location", location);
        weatherData.put("current", current);
        weatherData.put("hourly", hourly);
        weatherData.put("high", high);
        weatherData.put("low", low);
        weatherData.put("timestamp", Instant.now().toString());
        return weatherData;
    }

    /**
     * Parses the hourly time and temperature arrays from the JSON response.
     */
    private List<Map<String, Object>> parseHourlyForecast(String json) {
        List<Map<String, Object>> hourly = new ArrayList<>();

        try {
            String hourlyBlock = extractBlock(json, "\"hourly\"");

            // Extract time array
            List<String> times = extractStringArray(hourlyBlock, "\"time\"");
            // Extract temperature_2m array
            List<Double> temps = extractDoubleArray(hourlyBlock, "\"temperature_2m\"");
            // Extract weather_code array
            List<Double> codes = extractDoubleArray(hourlyBlock, "\"weather_code\"");

            int count = Math.min(times.size(), temps.size());
            count = Math.min(count, 4); // limit to 4 entries

            for (int i = 0; i < count; i++) {
                Map<String, Object> entry = new LinkedHashMap<>();
                // Extract just the HH:MM from the ISO datetime string
                String time = times.get(i);
                if (time.contains("T")) {
                    time = time.substring(time.indexOf('T') + 1);
                    if (time.length() > 5) time = time.substring(0, 5);
                }
                entry.put("time", time);
                entry.put("temp", (int) Math.round(temps.get(i)));
                if (i < codes.size()) {
                    entry.put("condition", weatherCodeToCondition((int) Math.round(codes.get(i))));
                }
                hourly.add(entry);
            }
        } catch (Exception e) {
            // If parsing fails, return empty list
        }

        return hourly;
    }

    /**
     * Extracts a JSON object block starting after the given key.
     * Finds the opening '{' and matches it to the closing '}'.
     */
    private String extractBlock(String json, String key) {
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return "{}";
        int braceStart = json.indexOf('{', keyIdx + key.length());
        if (braceStart < 0) return "{}";
        int depth = 1;
        int pos = braceStart + 1;
        while (pos < json.length() && depth > 0) {
            char c = json.charAt(pos);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            pos++;
        }
        return json.substring(braceStart, pos);
    }

    /**
     * Extracts a JSON array of strings after the given key.
     */
    private List<String> extractStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return result;
        int bracketStart = json.indexOf('[', keyIdx + key.length());
        if (bracketStart < 0) return result;
        int bracketEnd = json.indexOf(']', bracketStart);
        if (bracketEnd < 0) return result;
        String arrayContent = json.substring(bracketStart + 1, bracketEnd);

        int pos = 0;
        while (pos < arrayContent.length()) {
            int qStart = arrayContent.indexOf('"', pos);
            if (qStart < 0) break;
            int qEnd = arrayContent.indexOf('"', qStart + 1);
            if (qEnd < 0) break;
            result.add(arrayContent.substring(qStart + 1, qEnd));
            pos = qEnd + 1;
        }
        return result;
    }

    /**
     * Extracts a JSON array of doubles after the given key.
     */
    private List<Double> extractDoubleArray(String json, String key) {
        List<Double> result = new ArrayList<>();
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return result;
        int bracketStart = json.indexOf('[', keyIdx + key.length());
        if (bracketStart < 0) return result;
        int bracketEnd = json.indexOf(']', bracketStart);
        if (bracketEnd < 0) return result;
        String arrayContent = json.substring(bracketStart + 1, bracketEnd).trim();
        if (arrayContent.isEmpty()) return result;

        for (String part : arrayContent.split(",")) {
            try {
                result.add(Double.parseDouble(part.trim()));
            } catch (NumberFormatException e) {
                // skip non-numeric entries
            }
        }
        return result;
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
}
