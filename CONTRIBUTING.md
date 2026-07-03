# Contributing

Campal is published as a product prototype and open showcase.

## Before You Open a PR

- keep the project positioned around product vision and real-world social connection
- avoid reintroducing internal-only docs, operation notes, review notes, or rollout checklists
- do not commit secrets, `.env` files, database backups, perf result artifacts, or local runtime dumps
- prefer small, focused pull requests with a clear product or engineering purpose

## Contribution Guidelines

- product-facing copy should stay consistent with the repository positioning in `README.md`
- public docs should remain lightweight; internal development notes belong in local-only files
- if a change touches credentials, uploads, backups, or user data handling, explain the safety impact in the PR
- do not add Docker or deployment automation unless it is explicitly requested for the public repo

## Reporting Issues

- use issues for bugs, broken setup steps, and product feedback
- include reproduction steps when reporting implementation problems
- avoid posting secrets, personal data, or private infrastructure details in public issues
