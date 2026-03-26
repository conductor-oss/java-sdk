package sagapattern;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sagapattern.workers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that validates worker-to-worker data flow in the
 * booking saga, including:
 *   - Happy path: flight -> hotel -> payment all succeed
 *   - Compensation runs in reverse on failure
 *   - Partial failure: hotel fails after flight succeeds
 *   - Every book worker has a matching cancel worker
 */
class SagaIntegrationTest {

    @BeforeEach
    void setUp() {
        BookingStore.clear();
    }

    @Test
    void happyPathBookingAllSucceeds() {
        String tripId = "TRIP-HAPPY";

        // Book flight
        BookFlightWorker flightWorker = new BookFlightWorker();
        TaskResult flightResult = flightWorker.execute(taskWith(Map.of("tripId", tripId)));
        assertEquals(TaskResult.Status.COMPLETED, flightResult.getStatus());
        String bookingId = (String) flightResult.getOutputData().get("bookingId");
        assertEquals("FLT-TRIP-HAPPY", bookingId);

        // Reserve hotel
        ReserveHotelWorker hotelWorker = new ReserveHotelWorker();
        TaskResult hotelResult = hotelWorker.execute(taskWith(Map.of("tripId", tripId)));
        assertEquals(TaskResult.Status.COMPLETED, hotelResult.getStatus());
        String reservationId = (String) hotelResult.getOutputData().get("reservationId");
        assertEquals("HTL-TRIP-HAPPY", reservationId);

        // Charge payment
        ChargePaymentWorker paymentWorker = new ChargePaymentWorker();
        TaskResult paymentResult = paymentWorker.execute(taskWith(Map.of("tripId", tripId)));
        assertEquals(TaskResult.Status.COMPLETED, paymentResult.getStatus());
        assertEquals("success", paymentResult.getOutputData().get("status"));

        // Verify all bookings exist
        assertTrue(BookingStore.hasFlightBooking("FLT-TRIP-HAPPY"));
        assertTrue(BookingStore.hasHotelReservation("HTL-TRIP-HAPPY"));
        assertTrue(BookingStore.hasPaymentTransaction("TXN-TRIP-HAPPY"));

        // Verify action ordering
        List<String> log = BookingStore.getActionLog();
        assertEquals("BOOK_FLIGHT:FLT-TRIP-HAPPY", log.get(0));
        assertEquals("RESERVE_HOTEL:HTL-TRIP-HAPPY", log.get(1));
        assertEquals("CHARGE_PAYMENT:TXN-TRIP-HAPPY", log.get(2));
    }

    @Test
    void compensationRunsInReverseOnPaymentFailure() {
        String tripId = "TRIP-COMP";

        // Forward path: book flight, reserve hotel
        BookFlightWorker flightWorker = new BookFlightWorker();
        flightWorker.execute(taskWith(Map.of("tripId", tripId)));

        ReserveHotelWorker hotelWorker = new ReserveHotelWorker();
        hotelWorker.execute(taskWith(Map.of("tripId", tripId)));

        // Payment fails
        ChargePaymentWorker paymentWorker = new ChargePaymentWorker();
        TaskResult paymentResult = paymentWorker.execute(taskWith(Map.of("tripId", tripId, "shouldFail", true)));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, paymentResult.getStatus());

        // Verify bookings exist before compensation
        assertTrue(BookingStore.hasFlightBooking("FLT-TRIP-COMP"));
        assertTrue(BookingStore.hasHotelReservation("HTL-TRIP-COMP"));
        assertFalse(BookingStore.hasPaymentTransaction("TXN-TRIP-COMP"));

        // Compensation in reverse order: cancel hotel, then cancel flight
        CancelHotelWorker cancelHotelWorker = new CancelHotelWorker();
        TaskResult cancelHotel = cancelHotelWorker.execute(taskWith(Map.of("tripId", tripId)));
        assertEquals(true, cancelHotel.getOutputData().get("removedFromStore"));

        CancelFlightWorker cancelFlightWorker = new CancelFlightWorker();
        TaskResult cancelFlight = cancelFlightWorker.execute(taskWith(Map.of("tripId", tripId)));
        assertEquals(true, cancelFlight.getOutputData().get("removedFromStore"));

        // Verify all bookings are cleaned up
        assertFalse(BookingStore.hasFlightBooking("FLT-TRIP-COMP"));
        assertFalse(BookingStore.hasHotelReservation("HTL-TRIP-COMP"));

        // Verify compensation ran in reverse order (hotel before flight)
        List<String> log = BookingStore.getActionLog();
        int cancelHotelIdx = log.indexOf("CANCEL_HOTEL:HTL-TRIP-COMP");
        int cancelFlightIdx = log.indexOf("CANCEL_FLIGHT:FLT-TRIP-COMP");
        assertTrue(cancelHotelIdx < cancelFlightIdx,
                "Hotel should be cancelled before flight (reverse order of booking)");
    }

    @Test
    void partialFailureHotelFailsAfterFlightSucceeds() {
        String tripId = "TRIP-PARTIAL";

        // Book flight succeeds
        BookFlightWorker flightWorker = new BookFlightWorker();
        TaskResult flightResult = flightWorker.execute(taskWith(Map.of("tripId", tripId)));
        assertEquals(TaskResult.Status.COMPLETED, flightResult.getStatus());
        assertTrue(BookingStore.hasFlightBooking("FLT-TRIP-PARTIAL"));

        // Reserve hotel fails
        ReserveHotelWorker hotelWorker = new ReserveHotelWorker();
        TaskResult hotelResult = hotelWorker.execute(taskWith(Map.of("tripId", tripId, "shouldFail", true)));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, hotelResult.getStatus());

        // Hotel should NOT be in store
        assertFalse(BookingStore.hasHotelReservation("HTL-TRIP-PARTIAL"));

        // Flight IS still in store (needs compensation)
        assertTrue(BookingStore.hasFlightBooking("FLT-TRIP-PARTIAL"));

        // Compensate: cancel the flight
        CancelFlightWorker cancelFlightWorker = new CancelFlightWorker();
        TaskResult cancelResult = cancelFlightWorker.execute(taskWith(Map.of("tripId", tripId)));
        assertEquals(true, cancelResult.getOutputData().get("removedFromStore"));

        // Verify everything is cleaned up
        assertFalse(BookingStore.hasFlightBooking("FLT-TRIP-PARTIAL"));
        assertFalse(BookingStore.hasHotelReservation("HTL-TRIP-PARTIAL"));
        assertFalse(BookingStore.hasPaymentTransaction("TXN-TRIP-PARTIAL"));
    }

    @Test
    void everyBookWorkerHasMatchingCancelWorker() {
        String tripId = "TRIP-MATCH";

        // Book all
        new BookFlightWorker().execute(taskWith(Map.of("tripId", tripId)));
        new ReserveHotelWorker().execute(taskWith(Map.of("tripId", tripId)));
        new ChargePaymentWorker().execute(taskWith(Map.of("tripId", tripId)));

        // Cancel all
        new RefundPaymentWorker().execute(taskWith(Map.of("tripId", tripId)));
        new CancelHotelWorker().execute(taskWith(Map.of("tripId", tripId)));
        new CancelFlightWorker().execute(taskWith(Map.of("tripId", tripId)));

        // All stores should be empty
        assertTrue(BookingStore.isEmpty());

        // Verify matching pairs exist in action log
        List<String> log = BookingStore.getActionLog();
        assertTrue(log.contains("BOOK_FLIGHT:FLT-TRIP-MATCH"));
        assertTrue(log.contains("CANCEL_FLIGHT:FLT-TRIP-MATCH"));
        assertTrue(log.contains("RESERVE_HOTEL:HTL-TRIP-MATCH"));
        assertTrue(log.contains("CANCEL_HOTEL:HTL-TRIP-MATCH"));
        assertTrue(log.contains("CHARGE_PAYMENT:TXN-TRIP-MATCH"));
        assertTrue(log.contains("REFUND_PAYMENT:TXN-TRIP-MATCH"));
    }

    @Test
    void fullCompensationWithPaymentRefund() {
        String tripId = "TRIP-FULL-COMP";

        // Forward: all succeed
        new BookFlightWorker().execute(taskWith(Map.of("tripId", tripId)));
        new ReserveHotelWorker().execute(taskWith(Map.of("tripId", tripId)));
        new ChargePaymentWorker().execute(taskWith(Map.of("tripId", tripId)));

        // All bookings exist
        assertTrue(BookingStore.hasFlightBooking("FLT-TRIP-FULL-COMP"));
        assertTrue(BookingStore.hasHotelReservation("HTL-TRIP-FULL-COMP"));
        assertTrue(BookingStore.hasPaymentTransaction("TXN-TRIP-FULL-COMP"));

        // Full compensation in reverse: refund, cancel hotel, cancel flight
        new RefundPaymentWorker().execute(taskWith(Map.of("tripId", tripId)));
        new CancelHotelWorker().execute(taskWith(Map.of("tripId", tripId)));
        new CancelFlightWorker().execute(taskWith(Map.of("tripId", tripId)));

        // Everything cleaned up
        assertTrue(BookingStore.isEmpty());

        // Verify reverse order of compensation
        List<String> log = BookingStore.getActionLog();
        int refundIdx = log.indexOf("REFUND_PAYMENT:TXN-TRIP-FULL-COMP");
        int cancelHotelIdx = log.indexOf("CANCEL_HOTEL:HTL-TRIP-FULL-COMP");
        int cancelFlightIdx = log.indexOf("CANCEL_FLIGHT:FLT-TRIP-FULL-COMP");
        assertTrue(refundIdx < cancelHotelIdx, "Refund should happen before hotel cancel");
        assertTrue(cancelHotelIdx < cancelFlightIdx, "Hotel cancel should happen before flight cancel");
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
