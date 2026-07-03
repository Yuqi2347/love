# Campal

[中文 README](README.md)

> Use virtual AI to drive real-world social connection.

Campal is an AI-powered campus social product prototype.  
We believe AI should not replace human connection. It should help people begin it.

In campus life, many people are not unwilling to socialize. They are stuck before the first step:

- they want to go out, but have no one to go with
- they notice someone, but do not know how to begin naturally
- they have thoughts, emotions, and opinions, but no shared space for resonance

Campal is not built to solve "online companionship."  
It is built to reduce the starting cost of real-world connection.

## Positioning

Campal is not trying to become another social app that keeps people online longer.

We see it as a **real-world social catalyst**:

- helping people move from "I want to go, but have no one to go with" to actual activities
- helping people move from "I felt something" to a more natural first interaction
- helping people understand each other in lower-pressure virtual space before meeting in real life

Our core belief is simple:

**Virtual space is the bridge. Real connection is the destination.**

Read the full vision in [docs/vision.md](docs/vision.md).

## Core Product Directions

Campal currently focuses on three product directions.

### 1. Invite

The invite module is designed for one of the most common campus frustrations:

**I want to do something, but I do not know who to ask.**

It is not just an event-posting tool. It turns the absence of an existing circle into a lightweight social entry point, where shared intent gathers people first.

### 2. Moment

`Moment` is designed for another common but easily lost scenario:

**You notice someone, but do not know how to begin.**

With AI-assisted matching, profile understanding, result interpretation, and date preparation, it reduces the awkwardness and pressure of the first step. The goal is not abstractly "better matching." The goal is to create a more realistic path for people who would otherwise pass each other by.

### 3. Feed

The feed is not just a content timeline.

It is the public layer of campus expression: daily moments, opinions, emotions, gossip, discussion, and resonance. In Campal, this matters because real-world connection often starts with context, not direct messaging.

## How We Use AI

We do not treat AI as a companionship substitute.

In Campal, AI is used to support real social movement:

- helping interpret expression, interests, and profiles
- lowering the psychological barrier of matching and icebreaking
- turning online understanding into offline action suggestions
- reducing awkwardness at the start of a relationship

In one sentence:

**AI should not keep people online longer. It should make people more willing to move toward each other.**

## Product Principles

- **Real-world first**: online interaction should serve offline connection
- **Solve the first step**: many social problems are initiation problems
- **Design relationship progression**: not just matching, but what happens after matching
- **AI as support, not substitution**: AI should provide context, confidence, and direction
- **Start from campus**: campus is a dense, emotional, high-overlap environment for social experimentation

## Relationship Flow

Campal is built around a relationship progression path:

1. Expression: users leave understandable traces through profiles, posts, interests, and behavior.
2. Discovery: users encounter people, topics, and activities they would not normally reach.
3. Initiation: invite flows, matching flows, and guided interaction lower the starting cost.
4. Transition: AI helps move from online understanding to offline action.
5. Connection: the goal is real interaction, not endless in-app retention.

## What This Repository Represents

This repository is best understood as an **open product showcase and working prototype**, not a polished commercial release.

It aims to demonstrate:

- a clear product thesis around AI-assisted campus social connection
- a set of features designed to serve that thesis
- a working prototype across backend, web, and admin surfaces

## Repository Structure

```text
.
├── campus-love-backend/   # Spring Boot backend
├── campus-love-frontend/  # Vue web app
├── campus-love-admin/     # Admin console
├── docs/                  # Public-facing documentation
└── README.en.md
```

## Technology

The project currently includes:

- Spring Boot
- Vue 3
- TypeScript
- MySQL
- Redis
- Flyway
- WebSocket

Technology is the implementation layer, not the central story.  
The main value of Campal is how product thinking, relationship design, and AI support are combined into a coherent prototype.

## Local Development

For setup details, see:

- [campus-love-backend](campus-love-backend/)
- [campus-love-frontend/README.md](campus-love-frontend/README.md)

Before running locally, prepare your own environment variables and third-party credentials. Do not use real production or personal secrets.

## Current Status

Campal is still evolving.  
This repository revolves around one question:

**Can AI help more young people start real social connection, instead of retreating further into virtual isolation?**
