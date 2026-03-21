# Lead Nurturing in Java with Conductor : Segment, Personalize, Send, and Track Nurture Campaigns

## Moving Leads Through the Funnel with the Right Message at the Right Time

Not every lead is ready to buy. A lead in the awareness stage needs educational content, while a lead in the decision stage needs case studies and pricing. Sending the same generic email to everyone wastes opportunities. Effective nurturing requires segmenting leads by where they are in the funnel and what they care about, then crafting personalized content for each segment, delivering it, and measuring whether it moved them closer to conversion.

This workflow runs one nurture cycle for a lead. The segmenter classifies the lead based on their stage (awareness, consideration, decision) and interests. The personalizer creates content tailored to that segment. educational blog posts for awareness leads, product comparisons for consideration leads, ROI calculators for decision leads. The sender delivers the personalized content. The tracker measures engagement (open, click, reply) to inform the next nurture cycle.

## The Solution

**You just write the segmentation, personalization, sending, and tracking workers. Conductor handles the nurture pipeline and engagement data flow.**

Four workers handle the nurture cycle. segmentation, personalization, sending, and tracking. The segmenter classifies the lead by funnel stage and interests. The personalizer tailors content for the assigned segment. The sender delivers the message. The tracker measures engagement. Conductor sequences the four steps and passes segment data, personalized content, and delivery confirmations between them automatically.

### What You Write: Workers

SegmentWorker classifies leads by funnel stage, PersonalizeWorker tailors content for each segment, SendWorker delivers the message, and TrackWorker measures engagement to inform the next cycle.

| Worker | Task | What It Does |
|---|---|---|
| **PersonalizeWorker** | `nur_personalize` | Creates tailored content for the lead's segment. educational posts, product comparisons, or ROI calculators. |
| **SegmentWorker** | `nur_segment` | Classifies the lead by funnel stage (awareness, consideration, decision) and interest areas. |
| **SendWorker** | `nur_send` | Delivers the personalized nurture content to the lead via email. |
| **TrackWorker** | `nur_track` | Measures engagement (opens, clicks, replies) on the delivered content. |

### The Workflow

```
nur_segment
 │
 ▼
nur_personalize
 │
 ▼
nur_send
 │
 ▼
nur_track

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
