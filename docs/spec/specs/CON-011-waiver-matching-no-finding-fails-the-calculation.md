# A waiver that matches no finding fails the calculation rather than silently doing nothing

<!-- markdownlint-disable MD036 -->

**Title**
A waiver that matches no finding fails the calculation rather than silently doing nothing

**Lens**: CON

**Status**: planned

**Description**
Every waiver dispatched to a calculation must match at least one emitted finding.
A waiver that matches none fails that API test's calculation: the result is reported as failed, and
the failure names the waiver that matched nothing — its API, its target and its criterion if it named
one.

This is a hard error, not a warning and not a dropped entry.
A calculation does not proceed to a verdict while holding a waiver it could not place, because the
only two explanations are both things the author needs to hear: the target was written wrong, or the
target no longer exists in the specification.

The check is per dispatched waiver, after `SW-041`'s selection has run over every criterion of
that API test.
A waiver is matched if any criterion in scope produced a finding for its target, so a waiver naming a
criterion the gate excluded from this calculation is not an error — the exclusion is the gate's
decision, and a waiver is allowed to anticipate a criterion that is not currently running.
An expired waiver is matched or not on the same terms as any other; expiry is `SW-043`'s question
and is answered separately.

Because the check is per dispatched waiver, a waiver whose `apiVersion` is `'*'` has to match in every
version it reaches.
A report covering two versions of one API, where the newer version no longer declares the waived
target, therefore fails on the newer one — and that is the intended reading, not a gap: a wildcard is
the only waiver whose target can disappear without its author editing anything, so it is the one that
most needs telling.

**Rationale**
A waiver that does nothing is worse than a waiver that is rejected, because it reads as protection.
Its author believes the target is exempt, the build is green for an unrelated reason, and the day the
real finding appears nobody looks at the waiver file — they look at the criterion.
The expensive version of this is the renamed path: the exemption was legitimate, the endpoint moved,
and the waiver now silences nothing while continuing to look like an accepted justification in the
repository.

`SW-030` is what turns this from a preference into something the system can actually guarantee.
Because a criterion emits a finding for every target it enumerated, a miss cannot mean "the target
exists but produced no finding this run" — the target space is fully represented, so "no finding
matched" means the target is not in the specification as written.
Without that guarantee this rule would produce false failures on exactly the APIs it was meant to
help.

Failing rather than warning is the choice this spec fixes, and it follows from where the warning
would have gone.
The surfaces that carry a calculation's outcome are a coverage ratio, a findings list and a JUnit
document; none of them has a channel for "your input was partly ignored" that a build server acts on.
A warning in `additionalInformation` would be read by a human looking at a passing criterion, which
is the one reader who has no reason to look.
Failing closed puts the message in front of the author at the only moment the fix is cheap.

Exempting the gate-excluded criterion is the one place strictness is relaxed, deliberately.
A repository's waiver list is written once and run against whatever gate the pipeline names, so
requiring every waiver to match a _running_ criterion would make switching to a narrower gate fail
builds for waivers that are simply dormant.
The target still has to exist; only the criterion is allowed to be absent.

Treating expiry separately keeps the two errors from masking each other.
An expired waiver whose target also vanished should report both facts, and a single combined check
would report whichever was tested first — most likely the expiry, hiding the structural problem that
outlives it.

**Verification Description**
A test triggers a calculation with a waiver for a path the specification does not contain and asserts
the API test fails, with the message naming that waiver.
A test does the same for a correct path with a response code the operation does not declare.
A test asserts a waiver that matched at least one finding of at least one criterion does not fail the
calculation, even when another criterion in the same API test produced no finding for it.
A test asserts a waiver naming a criterion the quality gate excluded does not fail the calculation.
A test asserts a calculation holding an unmatched waiver reaches no coverage verdict for that API
test — the failure is not reported alongside a ratio.
A test asserts a `'*'` waiver matching in one version of an API and not in another fails only the
calculation of the version where it matched nothing.

## Relations

**Related**

- [SW-041](SW-041-waiver-resolves-against-the-emitted-findings.md) — the selection whose
  empty result this constrains
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — amended: the complete enumeration that
  makes a miss meaningful
- [SW-040](SW-040-waiver-enters-only-on-the-calculation-request.md) — the trigger-time
  validation that catches the errors visible without a specification
- [SW-043](SW-043-expiry-judged-against-the-triggering-instant.md) — the separate expiry
  verdict
- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — the gate scoping that makes a dormant
  criterion's waiver acceptable
- [CON-009](CON-009-coverage-agrees-with-findings.md) — amended: the agreement obligation a
  verdict-less failed calculation does not violate
- [CON-002](CON-002-tolerate-dependency-outages.md) — the contrasting case: an outage is tolerated,
  a wrong input is not
