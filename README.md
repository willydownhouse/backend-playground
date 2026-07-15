# notification-system

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running Kafka locally

The app publishes `PostCreatedEvent` messages to Kafka. Start Kafka before running the application in dev mode.

If the container already exists but is stopped:

```shell script
docker start kafka
```

Check that it is running:

```shell script
docker ps
```

If you need to create it fresh, remove any old container first:

```shell script
docker rm -f kafka
```

Then run:

```shell script
docker run -d \
  --name kafka \
  -p 9092:9092 \
  -e KAFKA_CFG_NODE_ID=0 \
  -e KAFKA_CFG_PROCESS_ROLES=controller,broker \
  -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=0@localhost:9093 \
  -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  bitnami/kafka:3.3.2
```

The app connects to **`localhost:9092`**.

If something looks wrong, check the logs:

```shell script
docker logs kafka
```

### Inspecting topics and messages

Topics live **inside the Kafka broker** (the `kafka` container). They are created automatically when the app first publishes to them. This project uses:

| Topic | Purpose |
|-------|---------|
| `post-created` | Main event stream when a post is created |
| `post-created-dlq` | Dead-letter queue for events that could not be processed |

List all topics:

```shell script
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Describe a topic (partitions, replicas):

```shell script
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic post-created
```

Read messages from a topic (prints JSON payloads; `Ctrl+C` to stop):

```shell script
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic post-created \
  --from-beginning
```

Read only the latest messages (useful if the topic already has old data):

```shell script
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic post-created
```

Read a limited number of messages from the DLQ:

```shell script
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic post-created-dlq \
  --from-beginning \
  --max-messages 10
```

Delete a topic (removes all messages; the topic is recreated automatically on next publish):

```shell script
docker exec kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --delete \
  --topic post-created-dlq
```

Delete both project topics (one command per topic):

```shell script
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic post-created
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic post-created-dlq
```

> **Note:** Run these commands on your host machine; `docker exec` runs the Kafka CLI **inside** the container where the broker and topics live. The app connects from outside via `localhost:9092`.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

### Connecting to PostgreSQL in dev mode

In dev mode, Quarkus **Dev Services** starts a PostgreSQL container automatically. You do not configure the datasource yourself.

When `./mvnw quarkus:dev` starts, look for a log line like:

```text
Container is started (JDBC URL: jdbc:postgresql://localhost:50136/quarkus?loggerLevel=OFF)
```

Use the **host**, **port**, and **database** from that URL. Dev Services defaults:

| Setting | Value |
|---------|-------|
| Host | `localhost` |
| Port | from the JDBC URL (changes per run) |
| Database | `quarkus` |
| User | `quarkus` |
| Password | `quarkus` |

Connect with `psql` (replace `50136` with your port from the logs):

```shell script
psql -h localhost -p 50136 -U quarkus -d quarkus
```

Or pass the password inline:

```shell script
PGPASSWORD=quarkus psql -h localhost -p 50136 -U quarkus -d quarkus
```

**Useful `psql` commands:**

```sql
-- list databases
\l

-- list tables in the current database
\dt

-- describe a table (columns, indexes, constraints)
\d notifications

-- run a query
SELECT * FROM users;

-- quit
\q
```

This project's tables: `users`, `posts`, `notifications`.

> **Note:** The dev database container only runs while `quarkus:dev` is up. The mapped port changes when Dev Services starts a new container — always check the JDBC URL in the startup logs.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/notification-system-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Bean validation using Hibernate Validator and Jakarta Validation annotations
- Kotlin ([guide](https://quarkus.io/guides/kotlin)): Write your services in Kotlin
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
- Hibernate ORM with Panache and Kotlin ([guide](https://quarkus.io/guides/hibernate-orm-panache-kotlin)): Define your persistent model in Hibernate ORM with Panache
