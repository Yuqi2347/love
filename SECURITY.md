# Security Policy

## Scope

Campal is published as a product prototype and open showcase. It is not maintained as a production SaaS service, but security issues in the repository are still taken seriously.

## Please Do Not Publicly Commit

- API keys, JWT secrets, mail passwords, or any other credentials
- `.env` files or real environment values
- database backups, runtime dumps, or test data derived from real users
- private infrastructure details or internal operation documents

## Reporting a Vulnerability

If you discover a security issue, do not open a public issue with sensitive details.

Report it privately to the project maintainer through a direct channel you already have, and include:

- a short description of the issue
- affected files or modules
- impact assessment
- reproduction steps if safe to share privately

## Response Expectations

- secrets accidentally exposed in the repository should be treated as compromised and rotated
- fixes should prioritize removing exposure from the public branch and preventing future reintroduction
- when necessary, Git history may be rewritten to remove sensitive material from past commits
