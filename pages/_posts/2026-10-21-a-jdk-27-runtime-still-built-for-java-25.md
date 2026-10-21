---
title: 'Release 2.3.0: a JDK 27 runtime, still built for Java 25'
# Explicit, because this post shares a filename date with the 2.2.0 announcement and has to sort
# after it. A date in the future would be worse than imprecise: Jekyll's 'future' defaults to false
# and 'pages.yml' has no scheduled rebuild, so a post dated past the merge is silently left out of
# the build and reappears only on the next one — another 'pages/**' push, a tagged deployment, or a
# manual dispatch.
date: 2026-10-21 12:00:00 +0200
excerpt: >
  The JVM service images move to Temurin 27. The bytecode target does not move —
  the published artifacts are still compiled for Java 25, so consumers still
  only need a JDK 25. No Java source changed in this release, which is the
  point: if something looks wrong on it, 2.2.0 is the release to compare
  against.
tags:
  - release
  - operators
  - ci
---

2.3.0 changes one thing: the JVM the services run on.

It is a separate release from
[2.2.0]({% post_url 2026-10-21-a-waived-pass-never-reads-like-a-covered-one %}) by design, and the
two go out within a day of each other.
2.2.0 closed the waiver work — a feature touching the calculators, the report shape, the CLI and the
web UI.
2.3.0 touches nine `Dockerfile`s, six poms, the CI workflow and the development docs — and not one
line of Java.
Shipping them together would have meant one upgrade with two unrelated ways to go wrong.

**So if anything seems amiss after upgrading to 2.3.0, install 2.2.0 and check whether it
reproduces there.**
If it does, the waiver work is the place to look and the runtime is innocent.
If it does not, it is this release.
That one comparison is worth more than any bug report we could ask you to write, and it is the whole
reason these two releases are numbered separately.

## Runtime, not compile target

`maven.compiler.release` stays at 25.
Every published Maven artifact is still release-25 bytecode and loads on a JDK 25, so nothing
downstream has to move.
Release-25 bytecode on a 27 JVM is forward-compatible, which is what makes this additive rather
than a version bump you have to follow.

The three axes are now independent, and only one of them moved:

| Axis                         | 2.2.0 | 2.3.0 | Who it affects                           |
| ---------------------------- | ----- | ----- | ---------------------------------------- |
| Bytecode target (`release`)  | 25    | 25    | Anyone consuming the published artifacts |
| JVM in the service images    | 25    | 27    | Anyone running the containers            |
| Native-image build toolchain | 25    | 25    | The four native services                 |

Local development stays on a JDK 25, which is the bytecode target.
`DEVELOPMENT.md` says so, and now also says CI runs the suites on both.

## What moved

- **The three JVM images Snow-White publishes** — `api-gateway`, `api-sync-job` and
  `openapi-coverage-stream` — get a 27 JVM, but not a 27 base image.
  Each one builds a custom `jlink` runtime in an `eclipse-temurin:27-jdk-alpine` stage and copies it
  onto a final `alpine:3.22.1`, which does not change.
  The `jdeps` step that computes each one's module set now runs `--multi-release 27`.
- **The other six `Dockerfile`s** do take `eclipse-temurin:27-jdk-alpine` as their final base: the
  four native services' fallback `Dockerfile`s (see below), and the two reference modules
  `example-spring-boot` and `example-snow-white-openapi-generator`.

That is the entire runtime change.
No Java source changed to accommodate it.

Five Dependabot commits ride along in the tag, having landed before the runtime work merged.
Two of them reach a shipped artifact: gRPC `1.84.3` to `1.85.0` in `otel-event-filter-stream`, and
the CLI's `undici` `8.11.2` to `8.12.1`.
The other three touch only the build — `pitest-maven`, `actions/setup-java` and `mikefarah/yq` — as
does the `eslint-plugin-jsdoc` bump that came with `undici`.
So if the comparison against 2.2.0 points at this release and the JVM is not the cause, those first
two are the next place to look.

## Preview features are out, and had to be

`api-sync-job` carried `--enable-preview` in four places: a compiler argument, the surefire and
failsafe `argLine`s, and its container `ENTRYPOINT`.
All four are gone, and no module may compile with the flag now.

The compiler argument is the one that forced this.
Preview class files are version-locked: javac rejects `--enable-preview` unless `--release` equals
the JDK doing the compiling, and a 27 JVM refuses to load classes marked preview-for-25.
A bytecode target of 25 and a runtime of 27 are therefore incompatible with preview features, full
stop.
The other three are launch flags, and each was harmless on its own — the launcher accepts
`--enable-preview` unconditionally, and it is the class files that are version-locked.
They went because, once the compiler argument was gone, there was nothing left for them to enable.

Removing the flag changed no Java source, which means nothing in the module still needed a preview
feature — it had outlived whatever it was added for.
The 27 leg of `Build and Test` is what would have caught it otherwise: `--release 25` together with
`--enable-preview` is rejected outright by a javac 27.

## The native images stay on 25

The four services published as GraalVM native images — `api-index-api`,
`otel-event-filter-stream`, `quality-gate-api` and `report-coordinator-api` — get no 27 JVM, because
a native image embeds SubstrateVM and carries no JDK at all.
Their `Dockerfile`s moved to the 27 base with the rest, but those are a fallback that neither CI nor
the release pipeline builds; the published artifact comes from the buildpack.

What the image build pins is the compiler, via the buildpack's `BP_JVM_VERSION`, which now reads a
dedicated `native.java.version` property instead of inheriting the bytecode target.

It is pinned to 25, which is a ceiling rather than a choice.
The bellsoft-liberica buildpack publishes `jdk` and `jre` for 27.0.0, but `native-image-svm` — the
dependency a native build actually resolves — only up to 25.0.4, on both architectures.
`BP_JVM_VERSION=27` fails dependency resolution.
Splitting the property out is what makes that a one-line edit once a 27 NIK is published, instead of
a change that would have dragged the bytecode target along with it.

Worth noting that `BP_JVM_VERSION` is not the only JDK involved: the Spring Boot AOT processing that
runs before the native compile uses the local JDK, so a native build straddles two.
`build.yml` says so at the `native-tests` JDK step.

## One thing an operator has to do by hand

`Build and Test` becomes a two-leg matrix in this release, one leg per JDK.
Both legs compile the same `--release 25` sources and run the unit and integration suites, one on a
25 JVM and one on 27 — a matrix rather than two jobs, because two jobs eventually drift apart.

The cost is the check name.
The legs report as `Build and Test (25)` and `Build and Test (27)`, and nothing in a repository can
apply that rename — the default-branch ruleset lives in GitHub settings.
If you run a fork with a required plain `Build and Test`, it will never report again: require the
two named contexts instead, and rebase open pull requests onto a head that produces them.

Worth knowing about the coverage those legs give: each pairs javac N with JVM N, so the combination
that actually ships — a javac-25 jar on the temurin-27 runtime — is exercised by no single job.
`build.yml` says so at the `app-tests` JDK step.
That gap is the other reason this release is isolated.

## Non-LTS, with a date on it

Java 27 is an interim release, not an LTS.
Adoptium's End of Availability for it is March 2027, so this pin needs revisiting before then —
forward to 28, or back to the 25 LTS.
The bytecode target staying on an LTS is what keeps that a decision about containers rather than
about compatibility.

## Upgrading

1. **Nothing to do if you consume the Maven artifacts.** They are unchanged: release-25 bytecode,
   JDK 25 and later.
2. **If you run `api-gateway`, `api-sync-job` or `openapi-coverage-stream`**, you get a 27 JVM.
   Check anything of yours that reads the JVM version, pins a Java agent to a major, or ships a
   `-XX` flag that moved.
   The four native images are unchanged — they have no JVM to bump.
3. **If you build images from these `Dockerfile`s in a fork**, all nine now pull a 27 JDK — six as
   their final base, and the three JVM services in the `jlink-builder` stage that produces their
   runtime.
   That includes the four native fallbacks Snow-White's own pipeline never builds.
4. **If you compile any module with `--enable-preview` in a fork, it has to go.** With the bytecode
   target at 25 and the runtime at 27, preview class files will not load.
5. **If you require a `Build and Test` status check**, update the ruleset to the two matrix contexts.
6. **If you build the native images**, the toolchain is still a 25 NIK.

And once more, because it is the only thing we are asking of anyone running this release: if
something is wrong, try 2.2.0 and tell us whether it is wrong there too.

The release notes for 2.3.0 will not help you here: release-please keeps `build:` and
`refactor(deps):` out of the changelog, so everything described above is invisible to it and the
notes hold little more than a link back to this post.
The honest list is the
[compare view](https://github.com/bbortt/snow-white/compare/v2.2.0...v2.3.0).
