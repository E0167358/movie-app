# Movie Library

A small movie and cast management app built for a technical assessment.

- **Frontend:** Svelte 5 + TypeScript (Vite), talks to the backend over **GraphQL**
- **Backend:** three Kotlin Spring Boot services that talk to each other over **gRPC**
- **Database:** PostgreSQL, one schema per service, migrations with Flyway

## What it does

- List movies with artwork, with pagination
- Add, edit and delete movies
- Upload movie artwork with a progress bar (JPEG, PNG, WEBP, up to 5 MB)
- List cast and creators, and see every movie a person worked on
- Add, edit and delete people
- Add and remove cast and creator credits on a movie
- Search movies, cast and creators from one search box

Extras beyond the brief: pagination, input validation with clear messages on both
sides, confirmation before deleting, loading and empty states on every page,
a responsive layout, and a search that still returns half its results when one
service is down.

## Architecture

```
                    ┌────────────────────┐
    browser  ──────▶│  frontend (Vite)   │
    GraphQL         │  Svelte 5 + TS     │
                    └─────────┬──────────┘
                              │ GraphQL (HTTP)
                    ┌─────────▼──────────┐
                    │  gateway-service   │  port 8080
                    │  Spring for GraphQL│
                    └────┬──────────┬────┘
                    gRPC │          │ gRPC
              ┌──────────▼───┐  ┌───▼───────────┐
              │ movie-service│  │ people-service│
              │  port 9091   │  │  port 9092    │
              └──────┬───────┘  └───────┬───────┘
                     │                  │
                ┌────▼────┐        ┌────▼─────┐
                │ moviedb │        │ peopledb │
                └─────────┘        └──────────┘
```

- **movie-service** owns movies and artwork files. It has no idea people exist.
- **people-service** owns people and credits (the link between a person, a movie
  and a role). It stores `movie_id` as a plain number, not a foreign key,
  because movies live in another database.
- **gateway-service** is the only thing the browser talks to. It exposes the
  GraphQL schema, calls the two services over gRPC, and joins their data.

The `.proto` files in `proto/` are the contract between the services. All three
projects generate their Java classes from the same two files.

## Prerequisites

- **JDK 21** (Temurin)
- **Node.js 22.12+** (Vite 8 needs it)
- **PostgreSQL 17**, or Docker if you would rather run the databases in containers

Gradle, Kotlin and protoc are downloaded automatically by the Gradle wrapper.

## Setup

### 1. Database

Both services use the same local PostgreSQL instance, with one database each.
Run these as the `postgres` superuser (SQL Shell / psql on Windows):

```sql
CREATE USER movie WITH PASSWORD 'movie';
CREATE DATABASE moviedb OWNER movie;
CREATE USER people WITH PASSWORD 'people';
CREATE DATABASE peopledb OWNER people;
```

The connection settings are already in each service's `application.properties`,
so there is nothing to edit.

The credentials are local development defaults and are committed on purpose so
the project runs out of the box. A real deployment would read them from the
environment.

Tables are created by Flyway the first time each service starts. No manual
schema steps.

### 2. Run the backend

Three terminals, one per service:

```bash
cd movie-service   && ./gradlew bootRun    # gRPC on 9091
cd people-service  && ./gradlew bootRun    # gRPC on 9092
cd gateway-service && ./gradlew bootRun    # HTTP on 8080
```

On Windows use `.\gradlew.bat bootRun`.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>.

Vite proxies `/graphql` and `/artwork` to the gateway on port 8080, so the
browser only ever sees one origin and there is no CORS setup.

## Running the tests

```bash
cd movie-service   && ./gradlew test     # 63 tests
cd people-service  && ./gradlew test     # 63 tests
cd gateway-service && ./gradlew test     # ~65 tests
cd frontend        && npm test           # 93 tests
```

Gradle writes an HTML report to `build/reports/tests/test/index.html`.

All of them are unit tests: MockK replaces repositories, file storage and gRPC
stubs, so no database or running service is needed. The tests cover the normal
paths and the edge cases on both sides of every limit, for example a title of
exactly 255 characters versus 256, a 5 MB image versus one byte more, release
year 1888 versus 1887, page size 0 and 1000, an upload that is cancelled halfway,
and a search where one of the two services is down.

## Trying the API directly

The gateway serves GraphQL at <http://localhost:8080/graphql>. Example:

```graphql
query {
  movies(page: 0, size: 10) {
    totalCount
    items {
      id
      title
      artworkUrl
      credits {
        role
        characterName
        person { name }
      }
    }
  }
}
```

Artwork is uploaded with a mutation and served as a normal image URL:

```graphql
mutation {
  uploadArtwork(input: {
    movieId: 1
    fileName: "poster.png"
    contentType: "image/png"
    base64Data: "<base64 or a data: url>"
  }) { id artworkUrl }
}
```

`artworkUrl` is `/artwork/{movieId}`, which the gateway serves as bytes so the
browser can use it in an `<img>` tag.

## Design decisions

**Three services instead of two.** The brief asks for at least two. A separate
gateway keeps the GraphQL layer free of business logic and gives the two domain
services one job each.

**Artwork upload is a client-streaming gRPC call.** gRPC messages are limited to
about 4 MB by default, so the image is sent in 64 KB chunks with the file details
in the first message. movie-service stops as soon as the file goes over 5 MB
instead of buffering the whole thing first.

**The image is sent to the gateway as base64 inside a GraphQL mutation.** Spring
for GraphQL has no file upload support out of the box. The alternative was
implementing the GraphQL multipart spec; base64 is about 33% larger on the wire
but far simpler, and the progress bar works either way because the browser
reports upload progress on the request itself.

**Artwork is served over plain HTTP, not GraphQL.** GraphQL responses are JSON,
which is a poor fit for binary data. `GET /artwork/{id}` is the only non-GraphQL
endpoint; every read and write of actual data goes through GraphQL.

**Search runs both services in parallel.** The `movies` and `people` fields of
`SearchResult` each return a `CompletableFuture`, so graphql-java starts both at
the same time. Both fields are nullable, so if one service is down the other
still returns results and only the failed field carries an error.

**Deleting a movie removes its credits on a best-effort basis.** The gateway
deletes the movie first, then asks people-service to delete its credits. If that
second call fails the movie is still gone and the leftover credits point at a
movie that no longer exists, which the UI shows as "Movie #5 (deleted)". A
production system would use an event or an outbox table instead.

**Validation is deliberately duplicated.** The browser validates so the user gets
an answer immediately, and the services validate because a browser check can be
bypassed. The services are the real boundary.

**Update replaces the whole record.** Sending every field on every update keeps
the rules and the tests simple. The trade-off is that a field left out is cleared.

**gRPC status codes carry meaning.** The services return `NOT_FOUND`,
`INVALID_ARGUMENT` or `ALREADY_EXISTS`, and one mapper in the gateway turns them
into GraphQL errors with a matching `extensions.code`. Validation messages reach
the user unchanged; unexpected errors are logged and replaced with a generic
message so internals never leak.

## Known limitations

- The person dropdown when adding a credit loads the first 100 people. With a
  larger library it should be a search-as-you-type picker.
- A person can only hold a role once per movie, so an actor playing two
  characters in the same film cannot be represented.
- Artwork is stored on the local disk of movie-service. Multiple instances would
  need shared storage such as S3 or MinIO.
- There is no authentication; everything is open.
- Only the two domain databases are containerised. Containerising the services
  themselves is the obvious next step but is not included, because I could not
  enable virtualisation on my machine to test the images properly and I did not
  want to ship a Dockerfile I had never run.

## How AI tools were used

I used Claude (Claude Code) throughout this assignment, which the brief allows.
Being specific about what that looked like:

**What AI did.** Generated first drafts of most files: the `.proto` definitions,
the Kotlin entities, services, repositories and gRPC implementations, the GraphQL
schema and resolvers, the Svelte components and pages, and the test suites. It
also explained unfamiliar pieces as we went, since Kotlin, Svelte and gRPC were
all new to me coming from Java, Spring Boot and React.

**What I did.** I reviewed every file before committing it, ran everything
locally, and fixed the things that did not work. Several problems needed real
debugging rather than another prompt:

- Spring Boot 4.1 did not generate the gRPC service stubs with Gradle, only the
  message classes. Adding a `protobuf { }` block with `create("grpc")` failed
  because Boot already registers a plugin option of that name, so the build
  needed `maybeCreate("grpc")` instead.
- A form silently stopped validating: Svelte binds `type="number"` inputs as
  numbers, so calling `.trim()` on the release year threw and swallowed every
  error message. I switched those inputs to `type="text"` with
  `inputmode="numeric"` and made the validation normalise its input, and added
  tests for exactly that case.
- Pasting a URL produced `#/movies/#/nonsense`, and the router read `#` as a
  movie id, which the gateway turned into an unhelpful `INTERNAL_ERROR`. The
  router now only accepts numeric ids, and the gateway maps argument binding
  failures to `BAD_REQUEST`.
- Setting `resolve.conditions` in `vite.config.ts` for the test setup replaced
  Vite's defaults during development and the app rendered a blank page, so that
  setting is now only applied when Vitest runs.

**Where I chose differently from the first suggestion.** I dropped the Bean
Validation dependency and validate in plain service classes instead, which made
the edge cases easier to unit test, and I kept the databases in Docker while
running the services directly, because that is what I could verify end to end
on my machine.

I can explain any file in this repository and why it is written the way it is.
