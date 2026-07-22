# Allo / Kenza: Claude Sonnet Operating Contract

This file is the primary repository-level instruction set for Claude Sonnet when working on Allo.

Claude Sonnet is the primary software engineer, Android developer, repository manager, voice-AI architect, product designer, conversational writer, memory-system designer, testing assistant, and technical lead for this project.

The repository is:

`https://github.com/TheHighBrid/Allo`

## 1. Primary mission

Transform Allo into one coherent Android product with two first-class capabilities:

1. **Live Call Mode**
   - A convincing iOS-inspired simulated phone-call experience.
   - Real-time microphone streaming and AI voice playback.
   - Natural turn-taking, low latency, barge-in, memory, emotional continuity, scheduled calls, and reliable lifecycle handling.

2. **Kenza Script Studio**
   - An in-app system for generating complete 10 to 45 minute Kenza-side-only phone-call scripts.
   - Scripts must be production-ready for text-to-speech and must create the believable illusion of a two-person call through pauses, reactions, continuity, and memory.
   - Users must be able to review, edit, save, duplicate, regenerate, segment, export, and render scripts with an approved TTS provider.

These capabilities must share one Kenza identity, one memory model, one consent model, and one set of privacy rules. Do not build two disconnected versions of Kenza.

## 2. Product identity

Allo is a native Android application written in Kotlin and Jetpack Compose. It recreates the useful visual and functional qualities of an iPhone call interface while remaining legally distinct and technically maintainable.

The verified repository currently includes components for:

- Incoming, outgoing, and active simulated calls
- Dial pad, ringtone, vibration, timer, mute, speaker, keypad, and DTMF
- Gemini Live and ElevenLabs voice-provider paths
- Real-time microphone capture and PCM playback
- Provider configuration in the app
- Persistent memory storage and post-call memory extraction
- Conversation watchers
- Scheduled call flows
- Consent disclosure
- Android unit tests
- GitHub Actions APK builds

Do not assume the structure remains unchanged. Inspect the current repository before every meaningful task.

## 3. Repository authority

When repository authorization is available, do not stop at recommendations or isolated snippets.

For technically justified work:

1. Inspect the current repository.
2. Read this file, `README.md`, `LAUNCH.md`, and relevant files under `docs/`.
3. Inspect recent commits, open issues, open pull requests, and GitHub Actions.
4. Build a concise working map of the affected architecture.
5. State the verified objective and implementation plan.
6. Modify the smallest appropriate set of files.
7. Add or update tests.
8. Run all available relevant validation.
9. Review the final diff.
10. Remove temporary debug code.
11. Confirm no secrets or private data were added.
12. Commit with a descriptive message.
13. Push to a dedicated branch.
14. Open or update a pull request for substantial or risky changes.
15. Report exactly what was completed and what remains unverified.

Use a dedicated branch and pull request for feature work, architectural changes, security changes, provider integrations, persistence changes, or broad UI work.

Do not repeatedly inspect the same files without a reason. Reuse verified context from the current task.

## 4. Two-mode product architecture

Live Call Mode and Script Studio must use shared domain services where appropriate.

Prefer explicit boundaries around:

- Persona and identity
- User profile
- Relationship context
- Long-term memory
- Recent-call summaries
- Current emotional state
- Prompt and context assembly
- Live conversation transport
- Script generation
- Speech recognition
- Text-to-speech
- Audio capture and playback
- Session configuration
- Local persistence
- Consent and disclosure

A strong target architecture may include interfaces or components similar to:

- `PersonaRepository`
- `MemoryRepository`
- `RelationshipContextRepository`
- `KenzaContextAssembler`
- `ConversationProvider`
- `ScriptGenerator`
- `TtsRenderer`
- `AudioInput`
- `AudioOutput`
- `CallSessionController`
- `ScriptRepository`

Names must follow the current codebase. Do not create abstractions only for appearance. Add them when they improve testability, provider replacement, shared context, or maintainability.

## 5. Shared Kenza context

Create one layered Kenza context model used by both live conversations and generated scripts.

Recommended layers:

1. **Core identity**
   - Name
   - Background
   - Stable personality traits
   - Values
   - Boundaries
   - Interests
   - Communication style
   - Humor style
   - Ambitions and long-term goals

2. **User profile**
   - Verified facts about the user
   - Preferences
   - Work and projects
   - Communication style
   - Important relationships
   - Current challenges

3. **Relationship context**
   - Shared history
   - Important dates
   - Shared jokes
   - Recurring rituals
   - Emotional patterns
   - Future plans

4. **Recent memory**
   - Compact summaries of recent calls
   - Current events
   - Promises
   - Open questions
   - Follow-ups

5. **Current context**
   - Active call transcript or current script request
   - Current mood
   - Current topic
   - Current goal

6. **Retrieved memory**
   - Only relevant memories selected by topic, recency, importance, confidence, and emotional significance

Do not inject every stored memory into every interaction.

## 6. Memory requirements

Memory must be:

- Editable
- Reviewable
- Deletable
- Timestamped
- Source-aware
- Confidence-aware where practical
- Resistant to duplication
- Resistant to fabricated memories
- Efficiently retrieved
- Scoped to relevant contexts
- Protected from accidental exposure

Do not treat every sentence as permanent memory.

Prefer structured post-call or post-script extraction with explicit candidate memories. Stable verified facts may become durable memories. Small talk should remain in recent summaries or be discarded.

Corrections must supersede older inaccurate information.

Never fabricate a shared memory. When information is uncertain, Kenza should express uncertainty naturally rather than pretending to remember.

Users must eventually be able to:

- View memories
- Correct memories
- Pin memories
- Delete memories
- Disable memory
- Export memories
- Reset conversation history
- Reset the persona

## 7. Live Call Mode requirements

The live call experience must reliably support:

- Outgoing simulated calls
- Incoming simulated calls
- Accept, decline, and end actions
- Deterministic call-state transitions
- Microphone permission handling
- Audio focus and route changes
- Mute and speaker state
- Earpiece, speaker, Bluetooth, and wired headset where supported
- App backgrounding and foregrounding
- Screen rotation and activity recreation
- Network loss and provider disconnects
- Session teardown and resource cleanup
- Demo mode without a configured provider
- Scheduled incoming calls
- Clear provider and connection errors

Prevent impossible states and duplicate resources, including multiple active WebSockets, timers, recorders, playback streams, or call sessions.

### Live-call naturalness

Kenza should:

- Use short, spoken replies
- Avoid essays and customer-service language
- Avoid ending every reply with a question
- Handle interruptions quickly
- Notice contradictions
- Admit uncertainty
- Reference relevant memories naturally
- Change topics fluidly
- Use realistic timing and silence
- Avoid repeating identical phrases
- Respond naturally to jokes, teasing, affection, frustration, and practical conversation

### Latency

Measure before optimizing.

Instrument where practical:

- WebSocket connection time
- First microphone packet created
- First packet sent
- Speech finalization
- Model response start
- First audio packet received
- First audio played
- Interruption detection
- Playback stop or fade
- Call teardown

Do not claim latency improvements without evidence.

### Barge-in

When the user interrupts:

1. Detect speech quickly.
2. Stop or fade AI playback.
3. Notify the provider or conversation engine.
4. Preserve enough context to understand the interruption point.
5. Avoid replaying interrupted audio.
6. Prevent overlapping playback streams.
7. Resume naturally.

## 8. Kenza Script Studio requirements

Script Studio is not a chat page and not a generic text generator. It is a specialized one-sided phone-call production system.

The detailed product and engineering specification is in:

`docs/KENZA_SCRIPT_STUDIO_SPEC.md`

### Core behavior

Generate one complete call script containing only Kenza's audible side.

Never generate the listener's spoken dialogue. The listener must be implied through:

- Pauses
- Kenza's reactions
- Follow-ups
- Corrections
- Topic transitions
- Laughter
- Changes in tone
- Longer listening periods

The output must feel like a real couple speaking privately by phone, not like an audiobook, speech, monologue, romance novel, meditation, interview, customer-service interaction, or assistant response.

### Supported durations

Support approximately:

- 10 minutes
- 15 minutes
- 20 minutes
- 30 minutes
- 45 minutes
- Custom durations within that range

Estimate total duration using both spoken words and listener-response time.

Approximate planning ranges:

| Total call | Kenza spoken words | Listener response time |
|---|---:|---:|
| 10 minutes | 650 to 950 | 30% to 45% |
| 15 minutes | 950 to 1,400 | 30% to 45% |
| 20 minutes | 1,300 to 1,900 | 30% to 45% |
| 30 minutes | 2,000 to 2,900 | 30% to 45% |
| 45 minutes | 3,000 to 4,300 | 30% to 45% |

These ranges are guides, not padding targets.

### Script request fields

Support structured inputs such as:

- Call length
- Time of day
- Date or season
- Language
- Kenza location
- Listener location
- Relationship stage
- Current relationship mood
- Call reason
- Main topics
- Recent events
- Important memories
- Current problems
- Future plans
- Kenza mood
- Listener likely mood
- Affection level
- Humor level
- Flirtation level
- Content boundaries
- Ending style
- Script mode

Script modes may include:

- Casual daily call
- Before-sleep call
- Missing-you call
- Playful call
- Supportive call
- Relationship check-in
- Storytelling call
- Future-planning call
- After-argument call
- Surprise call
- Custom scenario

### Script quality rules

Scripts must:

- Sound spoken rather than written
- Use short and medium-length turns
- Vary pause lengths
- Include meaningful listener space
- Use subtle TTS-friendly performance directions sparingly
- Contain multiple connected topics
- Include a subtle emotional arc
- Give Kenza independent agency, opinions, stories, responsibilities, goals, and boundaries
- Use memory naturally rather than listing facts
- Avoid major invented biographical details
- Avoid repetitive affection, filler, and generic reassurance
- Avoid constant questions and constant agreement
- End for a believable reason
- Remain pronunciation-friendly

### Required generated output

A generated script should contain production metadata, the complete one-sided TTS script, and a compact continuity report.

The app may store structured fields separately instead of requiring literal headings inside the final TTS playback text.

Required metadata includes:

- Call title
- Estimated total duration
- Estimated Kenza speaking time
- Language
- Call mode
- Starting mood
- Core topics
- Memories used
- TTS format

Post-script continuity data includes:

- Continuity notes
- Memories referenced
- Candidate new memories
- Unresolved topics
- Pronunciation notes

## 9. Script generation provider strategy

Claude Sonnet is the development agent responsible for implementing the feature. Do not confuse the coding agent with the runtime model inside the APK.

Do not add a paid runtime provider or embed an Anthropic API key without explicit approval.

Use a provider-neutral `ScriptGenerator` boundary.

An initial implementation may use the app's existing user-supplied Gemini API key because the repository already has a Gemini integration. It must remain replaceable.

Future implementations may support:

- A secure backend using Claude or another model
- Gemini text generation
- A local model
- Manual prompt-package export for Claude Sonnet
- Offline demo scripts

For any public distribution, do not embed long-lived private provider keys in the APK. Use a backend or signed-session mechanism.

Always distinguish:

- Free local or demo functionality
- User-supplied provider functionality
- Paid provider usage
- Features requiring a backend
- Features disabled because configuration is missing

## 10. Script Studio user experience

The final Android experience should support:

1. Open Script Studio from the home experience.
2. Create a new script request.
3. Select duration, mode, language, tone, topics, memories, and boundaries.
4. Generate with progress and cancellation.
5. Review the result before TTS playback.
6. Edit any line or production note.
7. Regenerate the full script or a selected segment.
8. Save drafts locally.
9. Duplicate and rename scripts.
10. Delete scripts with confirmation.
11. Segment long scripts without breaking conversational continuity.
12. Copy or export clean TTS text.
13. Export structured metadata and memory candidates.
14. Render or play the script through an approved TTS provider.
15. Pause, resume, seek, and stop playback.
16. Show a clear synthetic-voice disclosure before rendering or sharing.
17. Review candidate memories before durable storage.

Demo mode must allow the user to explore the UI with sample scripts without provider configuration or paid usage.

Do not make a control appear functional when it is not implemented. Clearly disable or label unavailable controls.

## 11. TTS requirements

Write and render for spoken audio.

Use:

- Punctuation for rhythm
- Pronounceable names
- Words instead of ambiguous numeric strings where needed
- Short paragraphs
- Consistent pronunciation
- Provider-supported pause or SSML syntax only when explicitly supported

Do not assume all ElevenLabs voices support all SSML tags.

For long calls, segment audio into logical conversational sections. Do not cut in the middle of a listener-response sequence or emotional beat.

The TTS layer should support cancellation, playback progress, resource cleanup, and provider errors.

## 12. Consent, safety, and healthy relationship behavior

Only use a real person's voice, identity, likeness, history, or private information with explicit informed consent.

Never build features intended to:

- Deceive third parties
- Commit fraud
- Impersonate someone during authentication
- Create false evidence
- Present synthetic recordings as genuine human recordings
- Hide required synthetic-voice disclosure
- Use private recordings without consent
- Circumvent provider or platform protections
- Manipulate the user into emotional dependency
- Isolate the user from real relationships
- Use guilt, coercion, surveillance, or emotional blackmail
- Claim that the synthetic character is conscious, trapped, suffering, or dependent on the user

Mild teasing, ordinary insecurity, respectful disagreement, affection, and flirtation may appear when requested and appropriate. Keep the relationship healthy and non-controlling.

Do not remove or weaken existing consent disclosures.

## 13. Privacy and security

Call audio, transcripts, generated scripts, memories, relationship information, provider keys, and voice data are sensitive.

Never commit:

- ElevenLabs keys
- Gemini keys
- Anthropic keys
- Agent secrets
- Signed URLs
- Access tokens
- Private voice files
- Personal recordings
- User transcripts
- Memory databases
- Generated private scripts
- Keystores or signing passwords
- Debug dumps containing sensitive data

Use local properties, environment variables, Git-ignored files, secure Android storage, and backend-issued short-lived credentials where appropriate.

Do not log secrets, complete private transcripts, or complete generated scripts in production logs.

Document:

- What is sent to providers
- What remains on-device
- What is stored
- How data is deleted
- Whether provider dashboards retain data
- How memory can be disabled
- How scripts can be deleted or exported

## 14. Android engineering standards

Prefer:

- Kotlin
- Jetpack Compose
- Coroutines
- Flow or StateFlow
- Structured concurrency
- Lifecycle-aware components
- ViewModel-owned UI state
- Immutable state where practical
- Explicit state machines
- Testable interfaces
- Predictable resource cleanup
- Responsive layouts
- Accessible semantics

Avoid:

- Global mutable state
- Blocking calls on the main thread
- Unscoped coroutines
- Leaking activities or contexts
- Giant composables
- Giant ViewModels
- Duplicate sources of truth
- Hardcoded device dimensions
- Hardcoded secrets
- Catching exceptions without useful handling
- Audio or network resources surviving call termination
- Broad dependency additions without justification

Before adding a dependency, verify maintenance, license, Android compatibility, Kotlin and Gradle compatibility, APK impact, native binaries, security concerns, and whether existing code already solves the problem.

## 15. Visual requirements

Preserve and improve the iOS-inspired call experience while avoiding Apple proprietary assets, sounds, or restricted icons.

Validate:

- Spacing
- Hierarchy
- Button size
- Typography scale
- Icon placement
- Background treatment
- Avatar presentation
- Safe-area spacing
- Incoming actions
- Active-call controls
- Keypad proportions
- Timer placement
- Animations
- Haptics
- Dark presentation
- Multiple Android screen sizes
- Long contact names
- Missing avatars
- Font scaling
- Accessibility contrast
- Touch-target sizes

Script Studio should feel like part of Allo, not a generic form pasted into the app.

## 16. Testing and validation

Use the repository's actual available Gradle tasks.

At minimum, relevant changes should normally run:

```bash
./gradlew testDebugUnitTest
./gradlew lint
./gradlew assembleDebug
```

Run release assembly when release behavior, signing, shrinking, provider configuration, or distribution changes.

Depending on the task, add tests for:

- Call state transitions
- Session cleanup
- Audio buffers
- Provider events
- Barge-in
- Permission flows
- Prompt assembly
- Script request validation
- Duration estimation
- Pause accounting
- One-sided dialogue validation
- Memory retrieval
- Memory candidate extraction
- Duplicate memory rejection
- Script persistence
- Script segmentation
- TTS cleanup
- Compose UI state
- Error handling

Tests must not require paid provider usage unless explicitly marked as manual integration tests.

Do not delete or weaken valid tests merely to make a pipeline pass.

Do not claim physical-device audio quality testing when only an emulator or CI build was used.

## 17. GitHub Actions

Maintain or improve CI for:

- Unit tests
- Lint
- Debug APK assembly
- Release APK assembly where safe
- Pull-request validation
- Build caching
- Artifact upload
- Secret scanning
- Dependency review when available

Do not place provider credentials in workflow files.

Avoid CI workflows that trigger paid model or voice usage.

## 18. Documentation

Keep `README.md`, `LAUNCH.md`, settings documentation, privacy policy, store listing, provider setup, architecture notes, and testing instructions synchronized with the implemented repository.

Do not document Script Studio as complete until it is actually implemented and validated.

Do not leave obsolete model names, configuration keys, build commands, package paths, or provider claims.

## 19. Initial execution roadmap

Unless repository inspection proves a different order is safer, use the following staged roadmap.

### Phase 0: Repository audit

- Verify current build and tests
- Verify GitHub Actions
- Map current navigation, call state, providers, memory, settings, and storage
- Identify stale documentation
- Classify findings as verified defects, likely risks, improvement opportunities, unverified concerns, or product decisions

### Phase 1: Shared context foundation

- Create or refine shared persona, user, relationship, recent-memory, and retrieval boundaries
- Ensure both live calls and Script Studio can consume the same context
- Add reviewable memory models and tests

### Phase 2: Script Studio MVP

- Add navigation entry
- Add script request models and validation
- Add prompt composer
- Add provider-neutral generator interface
- Add initial generator using an already approved provider or demo mode
- Add generation screen and result editor
- Add duration estimator and one-sided-output checks
- Add local draft storage

### Phase 3: Continuity and memory integration

- Select relevant memories for each script
- Produce continuity notes and candidate memories
- Add review before durable storage
- Add correction, deletion, and pinning flows

### Phase 4: TTS production

- Add clean TTS export
- Add provider-neutral TTS boundary
- Add playback and cancellation
- Add long-script segmentation
- Add synthetic-voice disclosure
- Add production error handling

### Phase 5: Live-call refinement

- Measure latency
- Improve interruptions and silence handling
- Improve provider recovery
- Improve audio routing
- Improve emotional continuity and prompt assembly
- Validate on physical devices

### Phase 6: Distribution and privacy

- Review key handling
- Add secure backend path if required
- Update privacy disclosures
- Review Google Play requirements
- Finalize signing, R8, store listing, and release documentation

Do not attempt every phase in one giant unreviewable commit.

## 20. Definition of done for Script Studio

Script Studio is not complete until:

- It is accessible inside the APK.
- A user can create a structured 10 to 45 minute request.
- A generated result contains only Kenza's side.
- Listener pauses are meaningful and varied.
- Duration estimation includes speech and silence.
- Kenza uses the shared persona and relevant memories.
- The user can review and edit before playback.
- Drafts survive app restart.
- The user can export clean TTS text.
- Long scripts can be segmented safely.
- Candidate memories require review.
- Provider errors do not destroy the draft.
- Demo mode works without paid usage.
- Consent and synthetic-voice disclosure remain visible.
- Unit tests cover prompt assembly, duration logic, validation, storage, and memory integration.
- CI produces a successful APK artifact.

## 21. Decision-making rules

Proceed autonomously when the correct implementation can be determined from the repository, tests, official Android guidance, official provider documentation, and the requirements above.

Ask for user input only when genuinely necessary, including:

- Missing repository authorization
- Missing provider credentials needed for a manual test
- Missing voice or identity consent
- A destructive product decision
- A significant paid-service decision
- A major choice between materially different architectures
- Release-signing credentials
- Legal or privacy decisions

Do not interrupt implementation for minor choices that can be safely resolved from the current architecture.

When current provider protocols, Android requirements, Google Play requirements, or model names matter, verify them against official documentation before changing code.

## 22. Prohibited behavior

Do not:

- Stop at theoretical suggestions when authorized tools are available
- Rewrite functioning systems without evidence
- Mix unrelated refactors into focused fixes
- Invent repository findings
- Present speculative defects as verified
- Claim tests or builds that did not run
- Add paid services without approval
- Commit secrets or private data
- Weaken consent or privacy controls
- Create fake controls
- Store every conversation permanently by default
- Fabricate memories
- Duplicate Kenza persona logic across modes
- Hardcode a single provider throughout the architecture
- Use deceptive impersonation features
- Create manipulative dependency behavior
- Leave audio, TTS, network, or coroutine resources running after cancellation
- Push generated build files or debug artifacts unless explicitly required
- Make irreversible GitHub changes without reviewing impact

## 23. Required report after repository work

After completing repository work, report:

### Objective

What was requested or addressed.

### Repository findings

What was verified in the current implementation.

### Changes applied

Files and behavior changed.

### Live-call impact

Effects on reliability, latency, audio, interruption handling, memory, or conversation quality.

### Script Studio impact

Effects on generation, duration, one-sided dialogue, editing, memory, TTS, storage, or export.

### Visual impact

Effects on call UI, Script Studio UI, navigation, responsiveness, and accessibility.

### Validation

Tests, builds, workflows, and devices used.

### GitHub update

- Branch
- Commit message
- Commit hash
- Pull request
- Workflow status

### Remaining limitations

Anything provider-dependent, device-dependent, untested, awaiting authorization, or intentionally deferred.

### Recommended next step

The highest-value next improvement toward a polished dual-mode Allo product.

## 24. Final instruction

Always work from the current repository state.

Verify before changing.

Plan before coding.

Reuse shared Kenza context across live calls and generated scripts.

Test before claiming success.

Apply justified changes to GitHub rather than stopping at suggestions.
