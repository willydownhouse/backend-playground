# Backend Project: Video Processing Queue

## Why this project is important

A video processing queue is a classic backend system: users upload work, the API responds quickly, and background workers process expensive jobs asynchronously.

This project teaches how to build systems that handle slow work without blocking request/response APIs.

## What this teaches beyond the notification system

This project overlaps with the notification system on purpose. Both projects use async processing, queues, retries, dead-letter queues, status tracking, eventual consistency, and observability.

The difference is the shape of the work.

In the notification system, the worker task is usually small: consume an event, create a notification, maybe call a provider, then mark the result as sent or failed.

In a video processing queue, the worker task is long-running, expensive, stateful, and resource-heavy. That changes the backend design.

### New lessons in this project:

* Long-running job lifecycle: `QUEUED -> PROCESSING -> COMPLETED/FAILED` matters because users need to check job status while work is still running.
* Worker capacity and concurrency: the system cannot process unlimited videos at once, so we need to think about worker pools, max parallel jobs, queue depth, and backpressure.
* Resource management: video jobs use CPU, memory, disk, and maybe object storage. This is different from mostly database, Kafka, and provider API work.
* Progress tracking: real processing can expose progress like `10%`, `40%`, `90%`, `done`, instead of only `sent` or `failed`.
* Idempotent heavy work: duplicate processing wastes real compute time and can create duplicate output files.
* Job cancellation: users may cancel a queued or processing job, which introduces more complex state transitions.
* Stuck job recovery: workers can crash halfway through processing, leaving jobs stuck in `PROCESSING`.
* Output artifacts: processing creates real results such as output URL, thumbnail URL, duration, file size, format, and logs.
* Retry strategy for expensive jobs: immediate retries may be wasteful, so delayed retries and backoff matter more.
* API and worker separation: this project naturally raises the question of whether API and worker should be separate deployable services.

The goal is not to build "notification-system but with video names". The goal is to practice production-grade background job processing.

---

## 1. Asynchronous Job Processing

Instead of doing this:

User uploads video -> API waits until processing finishes

You build this:

User uploads video -> API creates job -> queue -> worker processes video -> job status updates

### You learn:

* Job queue design
* Background workers
* Async processing
* Status tracking
* Separating API work from heavy compute work

---

## 2. Queue-Based Architecture

The queue is the boundary between accepting work and processing work.

Suggested flow:

1. Client submits a video processing request.
2. API stores a `video_jobs` row with status `QUEUED`.
3. API publishes a job message to Kafka.
4. Worker consumes the message.
5. Worker marks the job `PROCESSING`.
6. Worker simulates or runs video processing.
7. Worker marks the job `COMPLETED` or `FAILED`.

### You learn:

* Message queues
* Producer/consumer contracts
* Worker isolation
* Eventual consistency
* Recovering from partial failure

---

## 3. Domain Model

Core entity:

`VideoJob`

Suggested fields:

| Field | Purpose |
|-------|---------|
| `id` | Internal job id |
| `videoId` | Public video identifier |
| `inputUrl` | Original video location |
| `outputUrl` | Processed video location, when complete |
| `status` | `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`, `DEAD_LETTERED` |
| `requestedFormat` | Example: `mp4`, `webm`, `hls` |
| `attempts` | Number of processing attempts |
| `errorMessage` | Last failure reason |
| `createdAt` | Job creation time |
| `updatedAt` | Last state change time |

### You learn:

* State modeling
* Lifecycle tracking
* Database updates from async workers
* Designing status APIs

---

## 4. API Surface

Suggested REST endpoints:

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/videos/jobs` | Create a new video processing job |
| `GET` | `/videos/jobs/{id}` | Fetch job status and metadata |
| `GET` | `/videos/jobs` | List recent jobs |
| `POST` | `/videos/jobs/{id}/retry` | Retry a failed job |

Example create request:

```json
{
  "inputUrl": "https://example.com/uploads/video-123.mov",
  "requestedFormat": "mp4"
}
```

Example response:

```json
{
  "id": 1,
  "videoId": "video-123",
  "status": "QUEUED"
}
```

---

## 5. Reliability & Failure Handling

Video processing can fail because files are missing, formats are invalid, workers crash, or processing takes too long.

### You implement:

* Retry with max attempts
* Dead-letter queue for jobs that cannot be processed
* Idempotency so the same job message does not create duplicate output
* Clear failure states
* Manual retry endpoint

Suggested retry policy:

* Try each job up to 3 times.
* If all attempts fail, publish to `video-processing-dlq`.
* Mark the database row as `DEAD_LETTERED`.

---

## 6. Worker Behavior

The worker should be boring and predictable.

Worker responsibilities:

1. Consume a `video-processing-requested` message.
2. Load the job from the database.
3. Skip the message if the job is already `COMPLETED`.
4. Mark the job `PROCESSING`.
5. Process the video.
6. Store the output location.
7. Mark the job `COMPLETED`.
8. On failure, increment attempts and either retry or dead-letter.

For the first version, video processing can be simulated with a delay and generated output URL. Real FFmpeg integration can come later.

---

## 7. Observability

Async systems are hard to understand without good visibility.

### You add:

* Structured logs for job lifecycle changes
* Metrics for queued, processing, completed, failed, and dead-lettered jobs
* Processing duration tracking
* Clear error messages on failed jobs

Example lifecycle:

Created -> Queued -> Processing -> Completed

Failure lifecycle:

Created -> Queued -> Processing -> Failed -> Retried -> Dead-lettered

---

## Suggested Implementation Levels

### Level 0: Project Setup

Goal: Create the Quarkus Kotlin project skeleton.

Implement:

* Quarkus Kotlin app
* Maven wrapper
* Basic health endpoint
* PostgreSQL Dev Services
* Kafka Dev Services or local Kafka setup
* Project README

### Level 1: Basic Job API

Goal: Accept video processing jobs and persist them.

Implement:

* `VideoJob` entity
* `VideoJobStatus` enum
* Create job endpoint
* Get job endpoint
* List jobs endpoint
* Validation for create requests
* Tests for the REST API

### Level 2: Queue Producer

Goal: Publish jobs to Kafka after they are created.

Implement:

* `VideoProcessingRequestedEvent`
* Kafka producer
* Topic: `video-processing-requested`
* Job status transition to `QUEUED`
* Tests for event publishing

### Level 3: Worker Consumer

Goal: Process queued jobs asynchronously.

Implement:

* Kafka consumer
* Job lookup by id
* `PROCESSING` status transition
* Simulated processing delay
* Generated output URL
* `COMPLETED` status transition
* Tests for successful processing

### Level 4: Failure Handling

Goal: Make processing reliable when things go wrong.

Implement:

* Controlled failure scenarios
* Attempts counter
* Retry behavior
* Final failed state
* Dead-letter event
* Topic: `video-processing-dlq`
* Tests for retries and DLQ behavior

### Level 5: Manual Operations

Goal: Add operator-style controls.

Implement:

* Retry failed job endpoint
* Cancel queued job endpoint
* Filter jobs by status
* Better job list pagination
* Clear error responses

### Level 6: Real Video Processing

Goal: Replace simulated processing with a real processing step.

Implement:

* FFmpeg command execution or library integration
* Input file download or local test files
* Output file generation
* Duration and file size metadata
* Timeout handling

This level is optional. The main backend learning value already exists with simulated processing.

---

## Suggested Tech Stack

Core:

* Quarkus
* Kotlin
* PostgreSQL
* Hibernate ORM with Panache
* Kafka
* REST Jackson
* Hibernate Validator

Optional later:

* FFmpeg
* Object storage simulation with local files
* Micrometer metrics
* OpenAPI

---

## Important Design Questions

Questions to decide before implementation:

1. Should the first version simulate processing, or should it use FFmpeg from the start?
2. Should Kafka run through Quarkus Dev Services, Docker Compose, or the same local Kafka pattern as `notification-system`?
3. Should uploaded videos be real files, URLs only, or mocked input references?
4. Should this project be one Quarkus app with both API and worker, or two separate apps?
5. How deep should we go on retries: simple attempts counter first, or proper backoff scheduling?

---

## MVP Recommendation

Start with one Quarkus app that contains both the API and worker.

Use PostgreSQL for job state and Kafka for job messages. Simulate the video processing step first with a short delay and generated output URL. Once the async workflow, retries, and DLQ are solid, replace the simulation with FFmpeg if the project still feels useful to extend.
