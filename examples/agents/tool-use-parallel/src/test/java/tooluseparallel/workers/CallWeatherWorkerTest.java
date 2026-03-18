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

class CallWeatherWorkerTest {

    private final CallWeatherWorker worker = new CallWeatherWorker();

    @Test
    void taskDefName() {
        assertEquals("tp_call_weather", worker.getTaskDefName());
    }

    @Test
    void returnsWeatherData() {
        Task task = taskWith(Map.of("location", "San Francisco, CA"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("weatherData"));
    }

    @Test
    void weatherDataContainsLocation() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("location", "San Francisco, CA"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> weatherData = (Map<String, Object>) result.getOutputData().get("weatherData");
        assertEquals("San Francisco, CA", weatherData.get("location"));
    }

    @Test
    void weatherDataContainsCurrentConditions() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("location", "San Francisco, CA"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> weatherData = (Map<String, Object>) result.getOutputData().get("weatherData");
        @SuppressWarnings("unchecked")
        Map<String, Object> current = (Map<String, Object>) weatherData.get("current");
        assertNotNull(current, "Current conditions should be present");
        assertNotNull(current.get("temp"), "Temperature should be present");
        assertNotNull(current.get("condition"), "Condition should be present");
    }

    @Test
    void weatherDataContainsHourlyEntries() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("location", "Boston, MA"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> weatherData = (Map<String, Object>) result.getOutputData().get("weatherData");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hourly = (List<Map<String, Object>>) weatherData.get("hourly");
        assertNotNull(hourly, "Hourly forecast should be present");
        assertTrue(hourly.size() >= 1 && hourly.size() <= 4,
                "Expected 1-4 hourly entries, got " + hourly.size());
    }

    @Test
    void weatherDataContainsHighAndLow() {
        assumeTrue(isNetworkAvailable(), "Network not available — skipping");

        Task task = taskWith(Map.of("location", "San Francisco, CA"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> weatherData = (Map<String, Object>) result.getOutputData().get("weatherData");
        assertNotNull(weatherData.get("high"), "High temperature should be present");
        assertNotNull(weatherData.get("low"), "Low temperature should be present");
    }

    @Test
    void returnsWeatherApiSource() {
        Task task = taskWith(Map.of("location", "Denver, CO"));
        TaskResult result = worker.execute(task);

        assertEquals("open-meteo.com", result.getOutputData().get("source"));
    }

    @Test
    void handlesNullLocation() {
        Map<String, Object> input = new HashMap<>();
        input.put("location", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> weatherData = (Map<String, Object>) result.getOutputData().get("weatherData");
        assertEquals("New York, NY", weatherData.get("location"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }

    /**
     * Checks whether a network connection to the Open-Meteo API is available.
     */
    private static boolean isNetworkAvailable() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://geocoding-api.open-meteo.com/v1/search?name=London&count=1"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
