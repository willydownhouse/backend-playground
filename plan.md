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

### Step 1 — In-app notifications for new posts (current focus)

**Goal:** When a post is created, persist in-app notification rows for other users. No email, no queue yet.

**Architecture (synchronous, single service):**

```
POST /posts  →  save Post  →  NotificationService  →  save Notification rows  →  return Post
GET /notifications?userId=  →  list unread/read notifications for that user
PATCH /notifications/{id}/read  →  mark one as read
```

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
    User.kt              -- Panache entity
    UserResource.kt      -- REST
  post/
    Post.kt
    CreatePostRequest.kt -- DTO + validation
    PostResource.kt
  notification/
    Notification.kt
    NotificationType.kt  -- enum: NEW_POST
    NotificationService.kt   -- fan-out logic (@ApplicationScoped)
    NotificationResource.kt
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
| Message queue | Level 2 |
| Idempotency / dedup | Level 2–3 |
| Pagination | Add when inbox queries get slow |

#### Definition of done

- [ ] Can seed 2–3 users via API
- [ ] Creating a post creates `N-1` notification rows
- [ ] Author does **not** receive a notification for their own post
- [ ] User can list their notifications
- [ ] User can mark a notification as read
- [ ] Access log shows requests in dev terminal

#### Suggested build order (one PR-sized chunk at a time)

1. `User` entity + `POST/GET /users` — learn Panache basics
2. `Post` entity + `POST /posts` — no notifications yet
3. `Notification` entity + `NotificationService` — wire into post creation
4. `GET /notifications` + `PATCH .../read` — complete the inbox
5. Seed script or test data — make manual testing easy

---

### Level 1 (Basic) — full scope

* REST API (Quarkus + Kotlin) ✅ started
* PostgreSQL + Panache
* **Step 1:** In-app notifications on new post ← *you are here*
* User notification preferences (opt out of NEW_POST)
* Email notifications for new posts
* Fake → real email provider

---

### Level 2 (Intermediate)

* Message queue (RabbitMQ / Kafka)
* Worker service
* Retry logic
* Docker setup

Architecture:
API → DB → Queue → Worker → Email provider

---

### Level 3 (Advanced / Senior-Level)

* OAuth2 authentication
* Rate limiting
* Idempotency keys
* Kubernetes deployment
* Monitoring & alerting
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
