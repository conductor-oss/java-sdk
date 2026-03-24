# Ordered Message Processing in Java Using Conductor : Receive, Sort by Sequence, Process in Order, Verify

## Messages Arrive Out of Order, but Must Be Processed Sequentially

A financial trading system emits order updates. placed, partially filled, fully filled, cancelled. These events arrive over a message queue that doesn't guarantee ordering. Processing a cancellation before the placement, or a fill before the partial fill, produces incorrect portfolio state. The sequence numbers are embedded in the messages, but reconstructing order from a shuffled batch requires buffering, sorting, and then processing each message one at a time.

Without guaranteed ordering, you'd build a resequencing buffer that holds messages until gaps are filled, implements timeout logic for missing sequences, and processes them strictly in order. That's a lot of state management for what should be a simple sort-then-process pipeline.

## The Solution

**You write the sorting and sequential processing logic. Conductor handles the ordering pipeline, retries, and sequence verification.**

`OprReceiveWorker` ingests the batch of out-of-order messages. `OprSortBySequenceWorker` resequences them by their partition key and sequence number, producing an ordered list. `OprProcessInOrderWorker` executes the business logic on each message strictly in sequence. ensuring the placement is processed before the fill, and the fill before the cancellation. `OprVerifyOrderWorker` confirms that no sequence gaps exist and that the processing order matches the expected sequence. Conductor records the received order, sorted order, and verification result for every batch.

### What You Write: Workers

Four workers enforce sequential processing: message reception, sequence-number sorting, in-order execution, and order verification, ensuring events like placements and fills are never processed out of sequence.

### The Workflow

```
opr_receive
 │
 ▼
opr_sort_by_sequence
 │
 ▼
opr_process_in_order
 │
 ▼
opr_verify_order

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
