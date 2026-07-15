# backend-playground

Backend learning playground for building the kind of systems that show up in real backend engineering work.

Each folder is a standalone app with its own build, README, and focused backend concept.

Default stack: **Quarkus + Kotlin**, with project-specific infrastructure added as needed.

## Projects

| # | Project | Folder | Focus | Status |
|---|---------|--------|-------|--------|
| 0 | Notification system | [notification-system](./notification-system/) | Events, Kafka, persistence, async consumers | In progress |
| 1 | Video processing queue | [video-processing-queue](./video-processing-queue/) | Job queues, workers, retries, processing state | Planned |
| 2 | Search autocomplete | `search-autocomplete` | Indexing, prefix search, ranking, low-latency reads | Planned |
| 3 | Real-time leaderboard | `real-time-leaderboard` | Sorted rankings, caching, real-time updates | Planned |
| 4 | Payment retry engine | `payment-retry-engine` | Idempotency, retries, backoff, failure handling | Planned |
| 5 | Email delivery service | `email-delivery-service` | Queues, templates, providers, delivery tracking | Planned |
| 6 | Analytics event pipeline | `analytics-event-pipeline` | Event ingestion, batching, stream processing | Planned |
| 7 | Collaborative text editor | `collaborative-text-editor` | Real-time sync, conflict handling, WebSockets | Planned |
| 8 | Distributed job runner | `distributed-job-runner` | Scheduling, leases, workers, distributed coordination | Planned |
| 9 | Social media feed | `social-media-feed` | Fanout, timelines, pagination, feed ranking | Planned |

## Getting started

Pick a project, `cd` into its folder, and follow that project's README.

```shell
cd notification-system
./mvnw quarkus:dev
```

New projects should live at the repo root and keep their own build files, source code, tests, and documentation inside their folder.
