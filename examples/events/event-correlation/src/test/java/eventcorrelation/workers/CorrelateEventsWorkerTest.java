package eventcorrelation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CorrelateEventsWorkerTest {

    private final CorrelateEventsWorker worker = new CorrelateEventsWorker();

    @Test
    void taskDefName() {
        assertEquals("ec_correlate_events", worker.getTaskDefName());
    }

    @Test
    void correlatesAllThreeEvents() {
        Map<String, Object> orderEvent = Map.of(
                "type", "order", "orderId", "ORD-7712", "customerId", "C-001",
                "amount", 599.99, "currency", "USD", "timestamp", "2026-01-15T10:00:00Z");
        Map<String, Object> paymentEvent = Map.of(
                "type", "payment", "paymentId", "PAY-3301", "orderId", "ORD-7712",
                "amount", 599.99, "method", "credit_card", "timestamp", "2026-01-15T10:01:00Z");
        Map<String, Object> shippingEvent = Map.of(
                "type", "shipping", "shipmentId", "SHP-5501", "orderId", "ORD-7712",
                "carrier", "FedEx", "trackingNumber", "FX-998877");

        Task task = taskWith(Map.of(
                "correlationId", "corr-fixed-001",
                "orderEvent", orderEvent,
                "paymentEvent", paymentEvent,
                "shippingEvent", shippingEvent));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("eventsCorrelated"));
        assertEquals(1.0, result.getOutputData().get("matchScore"));
    }

    @Test
    void correlatedDataContainsOrderInfo() {
        Map<String, Object> orderEvent = Map.of("orderId", "ORD-7712", "customerId", "C-001", "amount", 599.99);
        Map<String, Object> paymentEvent = Map.of("amount", 599.99, "method", "credit_card");
        Map<String, Object> shippingEvent = Map.of("carrier", "FedEx", "trackingNumber", "FX-998877");

        Task task = taskWith(Map.of(
                "correlationId", "corr-1",
                "orderEvent", orderEvent,
                "paymentEvent", paymentEvent,
                "shippingEvent", shippingEvent));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("correlatedData");
        assertEquals("ORD-7712", data.get("orderId"));
        assertEquals("C-001", data.get("customerId"));
        assertEquals(599.99, data.get("orderAmount"));
    }

    @Test
    void correlatedDataContainsPaymentInfo() {
        Map<String, Object> orderEvent = Map.of("orderId", "ORD-7712", "customerId", "C-001", "amount", 599.99);
        Map<String, Object> paymentEvent = Map.of("amount", 599.99, "method", "credit_card");
        Map<String, Object> shippingEvent = Map.of("carrier", "FedEx", "trackingNumber", "FX-998877");

        Task task = taskWith(Map.of(
                "correlationId", "corr-1",
                "orderEvent", orderEvent,
                "paymentEvent", paymentEvent,
                "shippingEvent", shippingEvent));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("correlatedData");
        assertEquals(599.99, data.get("paymentAmount"));
        assertEquals("credit_card", data.get("paymentMethod"));
    }

    @Test
    void correlatedDataContainsShippingInfo() {
        Map<String, Object> orderEvent = Map.of("orderId", "ORD-7712", "customerId", "C-001", "amount", 599.99);
        Map<String, Object> paymentEvent = Map.of("amount", 599.99, "method", "credit_card");
        Map<String, Object> shippingEvent = Map.of("carrier", "FedEx", "trackingNumber", "FX-998877");

        Task task = taskWith(Map.of(
                "correlationId", "corr-1",
                "orderEvent", orderEvent,
                "paymentEvent", paymentEvent,
                "shippingEvent", shippingEvent));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("correlatedData");
        assertEquals("FedEx", data.get("carrier"));
        assertEquals("FX-998877", data.get("trackingNumber"));
    }

    @Test
    void handlesNullOrderEvent() {
        Map<String, Object> paymentEvent = Map.of("amount", 599.99, "method", "credit_card");
        Map<String, Object> shippingEvent = Map.of("carrier", "FedEx", "trackingNumber", "FX-998877");

        Map<String, Object> input = new HashMap<>();
        input.put("correlationId", "corr-1");
        input.put("orderEvent", null);
        input.put("paymentEvent", paymentEvent);
        input.put("shippingEvent", shippingEvent);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("correlatedData");
        assertEquals("UNKNOWN", data.get("orderId"));
        assertEquals("UNKNOWN", data.get("customerId"));
        assertEquals(0.0, data.get("orderAmount"));
    }

    @Test
    void handlesNullPaymentEvent() {
        Map<String, Object> orderEvent = Map.of("orderId", "ORD-7712", "customerId", "C-001", "amount", 599.99);
        Map<String, Object> shippingEvent = Map.of("carrier", "FedEx", "trackingNumber", "FX-998877");

        Map<String, Object> input = new HashMap<>();
        input.put("correlationId", "corr-1");
        input.put("orderEvent", orderEvent);
        input.put("paymentEvent", null);
        input.put("shippingEvent", shippingEvent);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("correlatedData");
        assertEquals(0.0, data.get("paymentAmount"));
        assertEquals("unknown", data.get("paymentMethod"));
    }

    @Test
    void handlesNullShippingEvent() {
        Map<String, Object> orderEvent = Map.of("orderId", "ORD-7712", "customerId", "C-001", "amount", 599.99);
        Map<String, Object> paymentEvent = Map.of("amount", 599.99, "method", "credit_card");

        Map<String, Object> input = new HashMap<>();
        input.put("correlationId", "corr-1");
        input.put("orderEvent", orderEvent);
        input.put("paymentEvent", paymentEvent);
        input.put("shippingEvent", null);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("correlatedData");
        assertEquals("unknown", data.get("carrier"));
        assertEquals("unknown", data.get("trackingNumber"));
    }

    @Test
    void correlatedDataContainsAllExpectedFields() {
        Map<String, Object> orderEvent = Map.of("orderId", "ORD-7712", "customerId", "C-001", "amount", 599.99);
        Map<String, Object> paymentEvent = Map.of("amount", 599.99, "method", "credit_card");
        Map<String, Object> shippingEvent = Map.of("carrier", "FedEx", "trackingNumber", "FX-998877");

        Task task = taskWith(Map.of(
                "correlationId", "corr-1",
                "orderEvent", orderEvent,
                "paymentEvent", paymentEvent,
                "shippingEvent", shippingEvent));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("correlatedData");
        assertEquals(7, data.size());
        assertTrue(data.containsKey("orderId"));
        assertTrue(data.containsKey("customerId"));
        assertTrue(data.containsKey("orderAmount"));
        assertTrue(data.containsKey("paymentAmount"));
        assertTrue(data.containsKey("paymentMethod"));
        assertTrue(data.containsKey("carrier"));
        assertTrue(data.containsKey("trackingNumber"));
    }

    @Test
    void matchScoreIsAlwaysOne() {
        Map<String, Object> orderEvent = Map.of("orderId", "ORD-7712", "customerId", "C-001", "amount", 100.0);
        Map<String, Object> paymentEvent = Map.of("amount", 100.0, "method", "debit_card");
        Map<String, Object> shippingEvent = Map.of("carrier", "UPS", "trackingNumber", "UP-123");

        Task task = taskWith(Map.of(
                "correlationId", "corr-2",
                "orderEvent", orderEvent,
                "paymentEvent", paymentEvent,
                "shippingEvent", shippingEvent));
        TaskResult result = worker.execute(task);

        assertEquals(1.0, result.getOutputData().get("matchScore"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
