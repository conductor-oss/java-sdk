# Calendar Agent in Java Using Conductor : Parse Requests, Check Availability, Find Slots, Book Meetings

Calendar Agent. parse meeting request, check attendee calendars, find available slots, and book the meeting through a sequential pipeline. ## Meeting Scheduling Is a Constraint Satisfaction Problem

Finding a time that works for three people across different time zones with existing commitments is tedious. The agent needs to parse the request ("1-hour meeting with Alice and Bob, preferably next Tuesday afternoon"), check each attendee's calendar for existing events, find overlapping free windows that satisfy the duration and preference constraints, and book the meeting with proper invitations.

Each step depends on the previous one. you can't find available slots without knowing existing commitments, and you can't book without finding a valid slot. If the calendar API is temporarily unavailable, you need to retry the availability check without re-parsing the request. And if the booking fails because someone accepted another meeting in the meantime, you need to find the next available slot.

## The Solution

**You write the request parsing, calendar queries, slot-finding, and booking logic. Conductor handles the scheduling pipeline, retries on calendar API failures, and booking state.**

`ParseRequestWorker` extracts structured meeting parameters from the natural language request. attendees, duration, preferred date, and any constraints (morning only, avoid Fridays). `CheckCalendarWorker` queries each attendee's calendar and returns their existing commitments with busy/free blocks. `FindSlotsWorker` computes overlapping availability windows that satisfy the duration and preference constraints, ranking them by preference score. `BookMeetingWorker` creates the calendar event in the best available slot and sends invitations to all attendees. Conductor sequences these four steps and records each one's output for scheduling analytics.

### What You Write: Workers

Four workers handle scheduling. Parsing the meeting request, checking attendee calendars, finding available slots, and booking the meeting.

| Worker | Task | What It Does |
|---|---|---|
| **BookMeetingWorker** | `cl_book_meeting` | Books the selected meeting slot. creates the calendar event, sends invitations, and returns the confirmed meeting de.. |
| **CheckCalendarWorker** | `cl_check_calendar` | Checks calendar availability for all attendees across the specified date range. Returns per-date time slots with avai... |
| **FindSlotsWorker** | `cl_find_slots` | Finds the best meeting slots from the availability data, applying the caller's preferences (morning preference, avoid... |
| **ParseRequestWorker** | `cl_parse_request` | Parses a natural-language meeting request into structured data: attendees with timezones, duration, date range, prefe... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
cl_parse_request
 │
 ▼
cl_check_calendar
 │
 ▼
cl_find_slots
 │
 ▼
cl_book_meeting

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
