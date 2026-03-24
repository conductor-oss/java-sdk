# Appointment Scheduling

Healthcare appointment scheduling: check availability, book, confirm, remind

**Input:** `patientId`, `providerId`, `preferredDate`, `visitType` | **Timeout:** 1800s

## Pipeline

```
apt_check_availability
    │
apt_book
    │
apt_confirm
    │
apt_remind
```

## Workers

**BookWorker** (`apt_book`): Books an appointment slot for a patient.

Reads `slot`, `visitType`. Outputs `appointmentId`, `booked`, `bookedAt`.

**CheckAvailabilityWorker** (`apt_check_availability`): Checks provider availability and returns an available slot.

Reads `preferredDate`, `providerId`. Outputs `availableSlot`, `alternateSlots`.

**ConfirmWorker** (`apt_confirm`): Sends appointment confirmation to the patient.

Reads `appointmentId`. Outputs `confirmed`, `channel`.

**RemindWorker** (`apt_remind`): Schedules a reminder notification for the appointment.

Reads `slot`. Outputs `reminderSet`, `reminderTime`.

## Tests

**31 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
