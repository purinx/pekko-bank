# Pekko-Bank

Simplified accounting system implemented with actor programming

## Technical Configuration

- PostgreSQL
- Scala 3.7.2 (sbt)
- Pekko HTTP
  - for the HTTP server layer
- Pekko Typed
  - for typed actor orchestration
- Doobie
  - for query execution

## Documentation

All project documentation lives in `docs/`. Start with the specifications, then dive into references that match your task:

- [docs/spec/requirements.md](docs/spec/requirements.md) – read when validating product scope, business rules, or non-functional expectations.
- [docs/spec/api.md](docs/spec/api.md) – use while building API endpoints or integrating clients; contains request/response contracts and error semantics.
- [docs/spec/erd.md](docs/spec/erd.md) – consult during schema design or persistence work to keep table structure and relationships aligned.
- [docs/spec/domain.md](docs/spec/domain.md) – glossary of domain terms and modeling notes (currently WIP; update when domain modeling evolves).

Reserved directories:

- [docs/adr/](docs/adr/) – architecture decision records (add new ADRs here as decisions are made).
- [docs/manual/](docs/manual/) – space for future runbooks or operator/user manuals.
