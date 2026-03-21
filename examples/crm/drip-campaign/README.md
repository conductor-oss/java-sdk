# Drip Campaign in Java with Conductor : Enroll, Send, Track, and Graduate Contacts Through an Email Series

## Nurturing Leads with Automated Email Sequences

Drip campaigns send a sequence of emails over time to nurture a lead toward conversion. Each contact needs to be enrolled, receive the right emails in the right order, have their engagement tracked (did they open? click? reply?), and eventually graduate from the campaign. either because they converted or completed the series. Managing this manually across thousands of contacts is impossible.

This workflow models the drip campaign lifecycle for a single contact. The enroll worker registers the contact in the campaign and creates an enrollment record. The send worker delivers the email series to the contact's address. The engagement tracker measures how the contact interacted with the emails and produces an engagement score. The graduation worker evaluates the engagement score and decides whether the contact has graduated (converted or completed the series). Each step depends on the previous one. you cannot track engagement before sending, and you cannot graduate before measuring engagement.

## The Solution

**You just write the enrollment, email-sending, engagement-tracking, and graduation workers. Conductor handles the drip sequence and lifecycle tracking.**

Four workers handle the drip lifecycle. enrollment, email sending, engagement tracking, and graduation. The enroller creates a campaign enrollment record. The sender delivers the email series and reports the count. The tracker monitors opens, clicks, and replies to compute an engagement score. The graduation worker uses the score to determine if the contact should graduate. Conductor sequences the steps and routes enrollment IDs and engagement scores between them automatically.

### What You Write: Workers

EnrollWorker registers the contact, SendSeriesWorker delivers timed emails, TrackEngagementWorker measures opens and clicks, and GraduateWorker decides if the contact moves to sales or gets recycled.

| Worker | Task | What It Does |
|---|---|---|
| **EnrollWorker** | `drp_enroll` | Registers the contact in the drip campaign and creates an enrollment record with a sequence name. |
| **GraduateWorker** | `drp_graduate` | Evaluates the engagement score and decides if the contact graduates to sales or gets recycled. |
| **SendSeriesWorker** | `drp_send_series` | Delivers the sequence of timed drip emails to the contact's email address. |
| **TrackEngagementWorker** | `drp_track_engagement` | Measures email engagement: open rates, click-through rates, and replies across the series. |

### The Workflow

```
drp_enroll
 │
 ▼
drp_send_series
 │
 ▼
drp_track_engagement
 │
 ▼
drp_graduate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
