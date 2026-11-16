# ADR009 - Public Test Links and Participant Identity
* Date: Nov 16, 2026
* Status: [ACCEPTED]
* Author: Irina Mijatovic (drafted with Claude Code)

## Decision
KnowGauge has two audiences. **Administrators** use the app to manage the library and create, review and publish
tests. **Participants** take a published test through a link and never see the rest of the app.

- A test has **no shared link until an administrator creates one** on the Share tab (`POST
  /api/tests/{id}/shared-link`, published tests only). That sets a random, unguessable `public_token`. The link
  `/t/{token}` works only while the test is `PUBLISHED`; unpublishing it, or any other status, makes it answer 404.
- **Regenerating** replaces the token, so the old link stops working at once. Attempts already started keep
  working (they have their own token), and personal invitations are not affected. Creating and regenerating are
  recorded as `SHARED_LINK_CREATED` / `SHARED_LINK_REGENERATED` test events.
- Participants are identified by what they enter on the start screen: **full name** (required) and **email**
  (required, the identity used to recognise repeat attempts and to filter results). Both are stored on the attempt
  (`participant_name`, `participant_email`). `user_id` becomes optional: it is set only for attempts started by a
  logged-in administrator through the admin API.
- Each attempt gets its own random `public_token`. The participant's browser reads and submits the attempt by that
  token, never by the numeric id, so one participant cannot open or submit another participant's attempt.
- Public endpoints live under `/api/public/**` and only ever expose what a participant needs: the test intro, the
  questions without the answer key, and after submitting the score and **their own answers marked right or
  wrong** (results page `/r/{attemptToken}`). The correct options and the explanations are never sent to
  participants; the full review stays on the administrator's Attempts page, so the answer key does not leak to the
  next participant.
- When an attempt with a participant email is scored, the server emails the score and the results link. The mail
  goes out after the scoring transaction commits (`AttemptResultNotifier`, `@TransactionalEventListener`); a failed
  send is only logged and never fails the submit.
- The shared link has **no attempt limit**: anyone can take the test as often as they like, and all attempts
  with the same email are grouped under that participant.
- **Personal invitations**: an administrator enters a list of emails and the server sends each address its own
  link `/i/{token}` (table `test_invitations`, one row per test and address). A personal link is **usable for one
  attempt** (a partial unique index on `attempts.invitation_id`); opening it again while the attempt runs
  continues that attempt, and once it is submitted the link answers "already used". The participant enters only
  the name: **the email is the invited address**, copied onto the attempt by the server and not editable.
- Inviting an address again re-sends the same link unless it was used. A failed send does not fail the request:
  the error is stored on the invitation (`send_error`) and the administrator can resend it.
- Mail is sent by the server over SMTP (Spring Mail, `spring.mail.*`, adapter module
  `knowgauge-service-infra-mail-springmail`) outside any DB transaction. Locally Mailpit catches every email
  (inbox http://localhost:8025). Links use `kg.invitations.link-base-url`, the public address of the UI.
- Per-test settings (show full review to participants, time limits) are a follow-up.

## Context
Attempts were first built for the single configured user (`kg.context.user-id`), started from the admin UI. The
intended use is different: an administrator publishes a test and sends a link to people who have no account, and
their results must be stored and filterable by person.

Numeric ids in URLs (`/tests/2/take`, `/attempts/5`) are guessable: anyone with one link could try others, read
other people's attempts or submit them. The admin UI must also not be reachable through a participant link.

Participants enter personal data (name, email). That is the minimum needed to tell results apart; anything more
would be collected without a need.

## Options Considered
### Option 1 (SELECTED): Token links plus participant details on the attempt
Random tokens for the test link and for each attempt; name and email entered on the start screen and stored on the
attempt.

#### Consequences
* Adopted because: participants need no account, and a link can be sent by any channel.
* Adopted because: tokens cannot be guessed from ids, and the public API cannot reach admin data.
* Adopted because: filtering by email covers "who took the test" until real users exist.
* Adopted despite: on the shared link the email is not verified; someone can enter another person's address.
  Personal invitations close that gap (the link only reaches the invited mailbox), so use them for tests where
  identity matters.
* Adopted despite: the server needs an SMTP server in production; without one invitations are created, but every
  send fails with its error shown.
* Adopted despite: a shared link stays valid until it is regenerated or the test is unpublished; anyone who got
  it meanwhile can take the test. Regenerate it when it reached the wrong people.

### Option 2: Participant accounts (login for everyone)
Participants register or are invited and log in before taking a test.

#### Consequences
* Rejected because: much heavier (identity provider, invitations, password resets) for people who take one test.
* Rejected despite: verified identity and a natural place for "my results".

### Option 3: Numeric test id in the link, no attempt token
Reuse `/tests/{id}/take` and `/attempts/{id}`.

#### Consequences
* Rejected because: ids are enumerable, so links to other tests and other people's attempts can be guessed.
* Rejected despite: no new columns.

## Advice
* The admin side must be protected (login, or at least a VPN or basic auth with only `/t/**` and `/api/public/**`
  open) before links are sent outside the organisation.
* Add a consent line on the start screen and decide how long participant data is kept (GDPR).
* Add rate limiting to `/api/public/**` before real use.
* Invitations are sent one by one in the request; for hundreds of addresses move sending to a background job.
* Configure SPF/DKIM for the sender domain, or invitations will land in spam.

## Supporting Material
* [ADR005 – Publishing Tests](ADR005-publish-tests.md)
* Migration: `knowgauge-service-rest-api/src/main/resources/db/migration/V1__create_initial_schema.sql` (`tests.public_token`, `attempts` participant columns, `test_invitations`)
* Mail adapter: `knowgauge-service-infra/knowgauge-service-infra-mail-springmail`
