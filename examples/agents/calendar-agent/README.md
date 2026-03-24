# Calendar Agent: Parse Meeting Request, Check Availability, Book

A user says "Schedule a 30-minute meeting with the team next Monday." The agent parses the request (detecting `"30"` in duration), checks calendar availability for `2026-03-10`, finds the best slot (scored at 0.95), and books the meeting with a `calendarLink: "https://calendar.company.com/event/" + meetingId`.

## Workflow

```
request, attendees, duration, preferredDate
  -> cl_parse_request -> cl_check_calendar -> cl_find_slots -> cl_book_meeting
```

## Workers

**ParseRequestWorker** (`cl_parse_request`) -- Extracts attendee emails, detects `duration.contains("30")` for 30-min meetings.

**CheckCalendarWorker** (`cl_check_calendar`) -- Returns availability for dates like `"2026-03-10"` with time slot details.

**FindSlotsWorker** (`cl_find_slots`) -- Finds the best slot with `score: 0.95`.

**BookMeetingWorker** (`cl_book_meeting`) -- Creates the meeting and returns `meetingId` and `calendarLink`.

## Tests

36 tests cover request parsing, availability checking, slot finding, and meeting booking.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
