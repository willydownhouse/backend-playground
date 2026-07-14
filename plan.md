# Backend Project: Notification System

## Why this project is important

A notification system is not just “sending emails”. It represents a real-world distributed backend system and teaches many core backend engineering concepts used in production systems.

---

## 1. Event-Driven Architecture

Instead of synchronous processing:

User action → send notification immediately

You build an event-based flow:

User Action → Event → Message Queue → Notification Service → Delivery (Email/SMS/Push)

### You learn:

* Event-driven design
* Decoupling services
* Asynchronous processing
* Scalability fundamentals

---

## 2. Message Queues & Background Workers

Instead of blocking API calls:

API waits for email delivery ❌

You use queues:

API → Queue → Worker → External provider

### You learn:

* Queues (RabbitMQ / Kafka / SQS)
* Background jobs
* Concurrency
* Retry mechanisms

---

## 3. Database Design

Typical schema:

users
notifications
notification_preferences

### You learn:

* Data modeling
* Indexing
* Query optimization
* Status tracking (pending/sent/failed)

---

## 4. Reliability & Failure Handling

Real systems fail.

### You implement:

* Retries with backoff
* Dead-letter queues
* Idempotency (avoid duplicate sends)
* Failure recovery strategies

---

## 5. External Integrations

You integrate real services like:

* Email (SendGrid / AWS SES)
* Push (Firebase Cloud Messaging)
* SMS (Twilio)

### You learn:

* API integration
* Rate limits
* Authentication
* Webhooks

---

## 6. Observability

You need to understand system behavior in production.

### You add:

* Logging
* Metrics
* Tracing
* Notification lifecycle tracking

Example flow:
Created → Queued → Processing → Sent → Failed

---

## Suggested Implementation Levels

### Domain scenario: Posts

**Trigger:** A user creates a post → other users should be notified.

This is a strong real-world scenario. The same pattern appears in:

* Social feeds (new post from someone you follow)
* GitHub (new issue / comment on a repo you watch)
* Slack / Teams (new message in a channel)
* Forums (new thread in a category you subscribe to)

It gives you a **concrete trigger event** (`PostCreated`) instead of abstract “send a notification” APIs. You learn to answer: *who* gets notified, *about what*, and *how* they read it.

**MVP simplification for Step 1:** skip auth and followers for now. When user A creates a post, notify **every other registered user**. Followers and preferences come in later steps.

---

### Step 1 — In-app notifications for new posts ✅ complete

**Goal:** When a post is created, persist in-app notification rows for other users.

**Architecture (implemented — async via Kafka):**

```
POST /posts  →  save Post  →  publish PostCreatedEvent  →  Kafka (post-created)
                                                              ↓
                                                    PostCreatedConsumer  →  save Notification rows

GET /notifications?userId=  →  list unread/read notifications for that user
PATCH /notifications/{id}/read  →  mark one as read
```

> **Note:** Step 1 originally planned synchronous fan-out in `NotificationService`. The implementation uses Kafka early (Level 2 preview). Fan-out runs in `PostCreatedConsumer`; `NotificationService` publishes the event.

#### Data model

```
users
  id (UUID or Long)
  username
  email          -- used later for email delivery

posts
  id
  author_id      → users.id
  title
  body
  created_at

notifications
  id
  recipient_id   → users.id   (who receives it)
  actor_id       → users.id   (who caused it — the post author)
  type           e.g. NEW_POST
  title          e.g. "Alice published a new post"
  body           e.g. post title or excerpt
  reference_id   → posts.id   (link back to the source)
  read           boolean, default false
  created_at
```

**Why `reference_id` + `type`:** real systems store *what* the notification is about so the client can deep-link (“open this post”). This is more realistic than a free-text message alone.

#### API endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/users` | Seed users (dev only; replace with auth later) |
| `GET` | `/users` | List users |
| `POST` | `/posts` | Create post + fan-out notifications |
| `GET` | `/posts` | List posts (optional, useful for testing) |
| `GET` | `/notifications?userId={id}` | Inbox for a user |
| `PATCH` | `/notifications/{id}/read` | Mark as read |

#### Request / response examples

Create post:

```json
POST /posts
{
  "authorId": "uuid-of-alice",
  "title": "Hello world",
  "body": "My first post"
}
```

Response `201` with the saved post.

Side effect: one `notifications` row per other user (e.g. if 3 users exist and Alice posts, Bob and Carol each get one notification).

List notifications:

```json
GET /notifications?userId=uuid-of-bob

[
  {
    "id": "...",
    "type": "NEW_POST",
    "title": "Alice published a new post",
    "body": "Hello world",
    "referenceId": "...",
    "read": false,
    "createdAt": "2026-06-27T12:00:00Z"
  }
]
```

#### Code structure (Quarkus / Kotlin)

```
com.learn
  user/
    User.kt, UserResource.kt, UserDtos.kt
  post/
    Post.kt, PostResource.kt, PostDtos.kt, PostCreatedEvent.kt
  notification/
    Notification.kt, NotificationType.kt
    NotificationService.kt      -- publishes PostCreatedEvent to Kafka
    PostCreatedConsumer.kt      -- fan-out logic (Kafka consumer)
    PostCreatedDeadLetter.kt    -- DLQ payload wrapper
    DeadLetterReason.kt         -- enum: AUTHOR_NOT_FOUND, ...
    NotificationResource.kt, NotificationDtos.kt
  dev/
    DevDataSeeder.kt              -- seeds users in dev profile
```

**Key learning moment:** keep fan-out logic in `NotificationService`, not inside the REST resource. The resource handles HTTP; the service handles business rules. Same pattern you’ll reuse when a queue replaces direct calls in Level 2.

#### Fan-out logic (pseudocode)

```
onPostCreated(post):
  otherUsers = User.find("id != ?1", post.authorId)
  for user in otherUsers:
    Notification.persist(
      recipient = user,
      actor = post.author,
      type = NEW_POST,
      title = "${author.username} published a new post",
      body = post.title,
      referenceId = post.id
    )
```

Wrap post creation + notification inserts in a **single transaction** (`@Transactional`) so you don’t get a post without notifications if something fails mid-way.

#### What you learn in Step 1

* REST design (status codes: 201, 400, 404)
* DTOs + Bean Validation (`@NotBlank`, etc.)
* JPA entities + Panache (`persist`, `find`, `list`)
* Relationships (`@ManyToOne` author, recipient)
* Service layer vs resource layer
* Transaction boundaries
* Manual testing with curl / HTTP client

#### Explicitly out of scope (later steps)

| Deferred | Why |
|----------|-----|
| Authentication | Adds complexity; use raw `userId` for now |
| Followers / audience rules | Step 4+ (preferences & targeting) |
| Email / push delivery | Level 1 Step 5–6 |
| Message queue | Level 2 ✅ (Kafka in use) |
| Idempotency / dedup | Step 2b ← *next* |
| Retry / exception DLQ | Step 2c |
| Pagination | Add when inbox queries get slow |

#### Definition of done

- [x] Can seed 2–3 users via API
- [x] Creating a post creates `N-1` notification rows
- [x] Author does **not** receive a notification for their own post
- [x] User can list their notifications
- [x] User can mark a notification as read
- [x] Access log shows requests in dev terminal

#### Suggested build order (one PR-sized chunk at a time)

1. ~~`User` entity + `POST/GET /users`~~ ✅
2. ~~`Post` entity + `POST/GET /posts`~~ ✅
3. ~~`Notification` entity + fan-out on post creation~~ ✅ (via Kafka consumer)
4. ~~`GET /notifications` + `PATCH .../read`~~ ✅
5. ~~Seed script or test data~~ ✅ (`DevDataSeeder` in dev)

---

### Step 2 — Consumer reliability: idempotency, retry, and DLQ ← *you are here*

**Goal:** Make `PostCreatedConsumer` production-safe: safe retries, no duplicate notifications, failed messages preserved.

**Current architecture:**

```
post-created  →  PostCreatedConsumer
                   ├─ author found     → fan-out notifications
                   └─ author missing   → publish to post-created-dlq (reason: AUTHOR_NOT_FOUND)
```

**Kafka topics:**

| Topic | Purpose |
|-------|---------|
| `post-created` | Main event stream |
| `post-created-dlq` | Unprocessable or failed events |

#### What’s done (Step 2a)

- [x] Kafka producer on post creation (`NotificationService` → `post-created`)
- [x] Kafka consumer for fan-out (`PostCreatedConsumer`)
- [x] Business-rule DLQ: `author == null` → `PostCreatedDeadLetter` on `post-created-dlq`
- [x] `PostCreatedDeadLetter` DTO + `DeadLetterReason` enum
- [x] README: Kafka setup, inspect topics/messages, delete topics

#### Next up (recommended order)

**Step 2b — Idempotency** *(do before or with retry)*

Problem: Kafka delivers at-least-once. If the consumer commits DB rows but crashes before acking the offset, redelivery creates **duplicate notifications**.

```
onPostCreated(event):
  for user in otherUsers:
    if notification already exists(recipient, referenceId, type):
      skip
    else:
      persist notification
```

Options:
- Check-before-insert in consumer
- Unique DB constraint on `(recipient_id, reference_id, type)` + handle duplicate gracefully

Definition of done:
- [ ] Reprocessing the same `PostCreatedEvent` does not create duplicate notification rows
- [ ] Fan-out is safe to run multiple times for the same event

**Step 2c — Retry with backoff**

Problem: transient failures (DB down, connection timeout) should retry; permanent failures should not loop forever.

```
Message arrives → try process
  → fail (exception)
  → retry with backoff (e.g. 1s, 2s, 4s)
  → still failing after N attempts
  → send to DLQ (reason: e.g. PROCESSING_FAILED)
  → ack and move on
```

Likely approach: SmallRye Kafka `failure-strategy` + retry config on `post-created-in` channel (separate from the explicit `author == null` DLQ path).

Definition of done:
- [ ] Transient DB errors are retried automatically (capped, with backoff)
- [ ] After max retries, message lands in `post-created-dlq` with failure metadata
- [ ] Consumer keeps processing subsequent messages (no poison-pill blocking)

**Step 2d — Optional polish**

- [ ] Expand `DeadLetterReason` (`PROCESSING_FAILED`, `MAX_RETRIES_EXCEEDED`, …)
- [ ] DLQ inspection / replay notes in README
- [ ] Tests for idempotency and retry paths

#### What you learn in Step 2

* At-least-once delivery and why idempotency matters
* Retry vs skip vs DLQ (transient vs permanent vs business-rule failures)
* Kafka topic patterns (main + DLQ)
* Transaction boundaries in async consumers (`@Transactional` rolls back mid-loop failures, but not post-commit redelivery)

---

### Level 1 (Basic) — full scope

* REST API (Quarkus + Kotlin) ✅
* PostgreSQL + Panache ✅
* **Step 1:** In-app notifications on new post ✅
* User notification preferences (opt out of NEW_POST)
* Email notifications for new posts
* Fake → real email provider

---

### Level 2 (Intermediate)

* Message queue (Kafka) ✅ started
* Consumer in same app (`PostCreatedConsumer`) ✅
* Business-rule DLQ (`author == null`) ✅
* **Step 2b:** Idempotency ← *next*
* **Step 2c:** Retry logic + exception DLQ
* Separate worker service (optional split later)
* Docker setup ✅ (Kafka via `docker run`; see README)

Architecture:
```
API → DB → Kafka (post-created) → Consumer → notifications DB
                                      └→ DLQ (post-created-dlq) on failure
```

---

### Level 3 (Advanced / Senior-Level)

* OAuth2 authentication
* Rate limiting
* Idempotency keys (API-level; consumer dedup covered in Step 2b)
* Kubernetes deployment
* Monitoring & alerting (DLQ depth, consumer lag)
* Distributed tracing

---

## Final Insight

A notification system is valuable because it teaches the same problems real backend systems solve:

* Scalability
* Reliability
* Asynchronous processing
* Fault tolerance
* System design thinking

It is significantly more valuable than a simple CRUD project for backend growth.
