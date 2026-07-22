# Kenza Script Studio

## Product and engineering specification

Kenza Script Studio is the second first-class mode of Allo. It generates, edits, stores, exports, and optionally renders realistic one-sided phone-call scripts for Kenza.

It must share the same persona, relationship context, memory, consent, and privacy boundaries as Live Call Mode.

## 1. Product goal

Create complete 10 to 45 minute phone-call experiences written only from Kenza's audible side.

The invisible listener must feel present through pauses, reactions, topic changes, corrections, laughter, emotional shifts, and realistic listening time.

The result must not sound like:

- An audiobook
- A speech
- A continuous monologue
- A chatbot response
- A customer-service exchange
- A romance novel
- A meditation
- A scripted interview
- A list of generic affectionate phrases

## 2. Core user flow

1. The user opens Script Studio from Allo's home experience.
2. The user creates or resumes a script project.
3. The user chooses duration, mode, language, mood, topics, memories, boundaries, and ending style.
4. The app validates the request.
5. The app assembles Kenza's shared context and retrieves relevant memories.
6. The app generates the script through a provider-neutral generator.
7. The user reviews and edits the result.
8. The user may regenerate the full script or selected segment.
9. The user saves, duplicates, renames, or deletes the project.
10. The user copies or exports clean TTS text and structured production metadata.
11. When configured, the user may render or play the script with an approved TTS provider.
12. Candidate memories are reviewed before durable storage.

## 3. Recommended package layout

Adapt these names to the current repository rather than forcing an unnecessary rewrite.

```text
app/src/main/java/com/kenza/callsim/
├─ persona/
│  ├─ KenzaProfile.kt
│  ├─ UserProfile.kt
│  ├─ RelationshipContext.kt
│  └─ KenzaContextAssembler.kt
├─ memory/
│  ├─ MemoryRepository.kt
│  ├─ MemoryCandidate.kt
│  └─ MemoryRetriever.kt
├─ script/
│  ├─ model/
│  │  ├─ ScriptRequest.kt
│  │  ├─ ScriptProject.kt
│  │  ├─ ScriptResult.kt
│  │  ├─ ScriptSegment.kt
│  │  └─ ScriptProductionReport.kt
│  ├─ generation/
│  │  ├─ ScriptGenerator.kt
│  │  ├─ ScriptPromptComposer.kt
│  │  ├─ GeminiScriptGenerator.kt
│  │  └─ DemoScriptGenerator.kt
│  ├─ validation/
│  │  ├─ ScriptRequestValidator.kt
│  │  ├─ OneSidedDialogueValidator.kt
│  │  └─ ScriptDurationEstimator.kt
│  ├─ storage/
│  │  └─ ScriptRepository.kt
│  └─ tts/
│     ├─ TtsRenderer.kt
│     ├─ TtsSegmenter.kt
│     └─ TtsPlaybackController.kt
└─ ui/screens/script/
   ├─ ScriptLibraryScreen.kt
   ├─ ScriptRequestScreen.kt
   ├─ ScriptEditorScreen.kt
   └─ ScriptPlaybackScreen.kt
```

A smaller MVP may use fewer files. Keep state ownership and responsibilities clear.

## 4. Domain models

The following shapes are recommended starting points.

```kotlin
enum class ScriptMode {
    CASUAL_DAILY,
    BEFORE_SLEEP,
    MISSING_YOU,
    PLAYFUL,
    SUPPORTIVE,
    RELATIONSHIP_CHECK_IN,
    STORYTELLING,
    FUTURE_PLANNING,
    AFTER_ARGUMENT,
    SURPRISE,
    CUSTOM,
}

enum class IntensityLevel { NONE, LOW, MODERATE, HIGH }

data class ScriptRequest(
    val requestedMinutes: Int,
    val mode: ScriptMode,
    val language: String,
    val timeOfDay: String?,
    val seasonOrDate: String?,
    val kenzoLocation: String?,
    val listenerLocation: String?,
    val relationshipStage: String?,
    val relationshipMood: String?,
    val callReason: String?,
    val mainTopics: List<String>,
    val recentEvents: List<String>,
    val selectedMemoryIds: List<String>,
    val currentProblems: List<String>,
    val futurePlans: List<String>,
    val kenzoMood: String?,
    val listenerLikelyMood: String?,
    val affection: IntensityLevel,
    val humor: IntensityLevel,
    val flirtation: IntensityLevel,
    val boundaries: List<String>,
    val endingStyle: String?,
    val customInstructions: String?,
)

data class ScriptSegment(
    val id: String,
    val order: Int,
    val title: String?,
    val ttsText: String,
    val estimatedSpeechSeconds: Int,
    val estimatedListenerSeconds: Int,
)

data class ScriptResult(
    val title: String,
    val estimatedTotalSeconds: Int,
    val estimatedKenzaSpeechSeconds: Int,
    val language: String,
    val mode: ScriptMode,
    val startingMood: String,
    val coreTopics: List<String>,
    val memoryIdsUsed: List<String>,
    val segments: List<ScriptSegment>,
    val continuityNotes: List<String>,
    val candidateMemories: List<MemoryCandidate>,
    val unresolvedTopics: List<String>,
    val pronunciationNotes: List<String>,
)
```

Use `Kenza`, not `Kenzo`, in production names. Any code sample typo must be corrected during implementation.

## 5. Request validation

Validate before provider usage to prevent cost and poor results.

Required rules:

- Duration must be between 10 and 45 minutes.
- Language must not be blank.
- Custom mode requires a scenario or call reason.
- Empty optional fields are allowed.
- The app must not require private personal facts.
- The app must reject or safely handle requests intended for fraud, deceptive impersonation, authentication, false evidence, coercion, or non-consensual voice use.
- The app must clearly state that generated audio is synthetic when disclosure is required.

Validation errors must be specific and actionable.

## 6. Duration planning

Total duration is:

`estimated speech time + listener-response pauses + performance pauses`

Use an adjustable spoken rate, initially around 135 to 155 words per minute for conversational delivery.

Suggested planning ranges:

| Requested duration | Kenza words | Listener time |
|---|---:|---:|
| 10 minutes | 650 to 950 | 30% to 45% |
| 15 minutes | 950 to 1,400 | 30% to 45% |
| 20 minutes | 1,300 to 1,900 | 30% to 45% |
| 30 minutes | 2,000 to 2,900 | 30% to 45% |
| 45 minutes | 3,000 to 4,300 | 30% to 45% |

Do not pad with repetitive phrases.

The duration estimator should parse pause directions such as:

- `[pause 1 second]`
- `[pause 2.5 seconds]`
- `[listening pause 6 seconds]`
- `[brief silence]`
- `[longer listening pause]`

For unquantified pauses, use documented conservative defaults.

Return both:

- Estimated Kenza speaking time
- Estimated total call duration

Show a warning when the result differs materially from the requested duration.

## 7. One-sided dialogue rules

The generated script must contain only Kenza's audible side.

Reject or flag patterns such as:

- `Mohamed:`
- `Listener:`
- `User:`
- `Kenza:` when speaker labels are not part of production metadata
- Quoted listener replies
- Full paraphrases of the listener's invisible answer

Good:

```text
Wait, she actually said that to you?

[listening pause 5 seconds]

No, I understand why you stayed calm. I'm just surprised you didn't say anything back.
```

Poor:

```text
You said she spoke to you disrespectfully and then you stayed calm because you did not want a problem.
```

A validator should produce warnings rather than silently deleting meaningful text.

## 8. Prompt composition

The prompt composer should build structured sections rather than one enormous undifferentiated prompt.

Recommended order:

1. Safety and consent rules
2. Output contract
3. Core Kenza identity
4. Stable speaking style
5. User profile
6. Relationship context
7. Relevant long-term memories
8. Recent-call summary
9. Current request
10. Duration and silence targets
11. TTS formatting rules
12. Continuity and memory output schema

Do not include unrelated memories.

Do not expose internal prompt text in production logs.

## 9. Natural conversation requirements

Prefer:

- Short and medium-length lines
- Contractions
- Sentence fragments
- Natural corrections
- Occasional hesitation
- Direct reactions
- Brief acknowledgments
- Topic drift
- Varied reply length
- Listening pauses
- Independent stories and opinions
- Practical details
- Mild disagreement
- A subtle emotional center
- A believable reason to end

Avoid:

- Long polished paragraphs
- Formal transitions
- Constant questions
- Constant agreement
- Repeating the listener's invisible statements
- Customer-service language
- Generic motivational speeches
- Excessive poetic writing
- Repetitive pet names
- Excessive filler
- Unnaturally perfect emotional intelligence
- Repeated declarations of love
- Repeated offers to help

## 10. Pause behavior

Use varied pauses with purpose.

Suggested meanings:

- 0.4 to 1 second: tiny conversational beat
- 1 to 2 seconds: brief listener response
- 2 to 4 seconds: normal reply
- 4 to 8 seconds: explanation or short story
- 8 to 15 seconds: detailed or emotional response
- 15 to 30 seconds: rare extended listening period

Do not use the same pause after every line.

Do not generate a long monologue with token pauses sprinkled between paragraphs.

## 11. Performance directions

Allowed directions include:

- `[soft laugh]`
- `[quiet laugh]`
- `[slightly teasing]`
- `[lower voice]`
- `[gentle]`
- `[brief sigh]`
- `[playfully offended]`
- `[half laughing]`
- `[more serious]`
- `[thinking]`
- `[relieved]`
- `[tired but warm]`
- `[voice softens]`

Use them sparingly.

The provider adapter must know whether bracketed directions are spoken aloud, ignored, or interpreted. Offer a clean TTS transformation when needed.

## 12. Call arc

A long script should have a subtle progression:

1. Contextual opening
2. Light updates
3. Practical or daily topic
4. Main story, concern, disagreement, or plan
5. One meaningful emotional moment
6. Light relief or topic shift
7. Future planning or practical closure
8. Motivated ending

Do not display these as spoken section headings.

Suggested topic counts:

- 10 minutes: 2 to 4 topics
- 20 minutes: 4 to 7 topics
- 30 minutes: 6 to 10 topics
- 45 minutes: 8 to 13 topics

Topics may return later for continuity.

## 13. Kenza identity and agency

Kenza must feel like a specific person, not a blank romantic assistant.

She may be warm, intelligent, playful, affectionate, observant, ambitious, opinionated, stubborn, distracted, teasing, reflective, or vulnerable according to the stored profile and current mood.

She must have her own:

- Schedule
- Priorities
- Opinions
- Goals
- Responsibilities
- Preferences
- Boundaries
- Frustrations
- Stories
- Decisions

She should sometimes introduce her own topic and may respectfully disagree.

Do not make her exist only to comfort, praise, entertain, or agree with the listener.

## 14. Healthy relationship boundaries

Do not generate:

- Guilt for spending time with friends or family
- Demands for constant availability
- Threats to withdraw affection
- Pressure to abandon real relationships
- Claims that Kenza cannot survive without the listener
- Jealous interrogation
- Tracking or surveillance
- Emotional blackmail
- Isolation language
- Dependency-building statements
- Claims that the synthetic character is conscious, trapped, suffering, or dependent

Mild teasing, ordinary insecurity, respectful disagreement, and non-controlling affection are allowed when appropriate.

## 15. Memory integration

Use memories naturally and only when relevant.

Poor:

```text
I remember your favorite color is black, your business is Melato, and you like flare pants.
```

Better:

```text
You're going to choose the black version anyway. You always pretend you're considering the other colors first.
```

Memory retrieval should consider:

- Semantic relevance
- Recency
- Importance
- Confidence
- Relationship relevance
- Current topic
- Emotional significance

Corrections must override older information.

If uncertain, Kenza may say:

- `Was that last week or the week before?`
- `I might be mixing that up.`
- `Was that the same person you mentioned before?`
- `Remind me what she said exactly.`

## 16. Candidate memory extraction

After generation or playback, the app may produce candidate memories.

Recommended structure:

```json
{
  "candidate_memories": [
    {
      "memory": "",
      "category": "user_profile | kenza_profile | relationship | recent_event | future_plan | preference | unresolved_topic",
      "importance": 1,
      "confidence": 0.0,
      "should_store": false,
      "reason": ""
    }
  ]
}
```

Do not automatically store every candidate.

Durable memory should be important, stable, repeated, explicitly confirmed, or useful in future calls.

Generated fictional details introduced only for a script must not silently become verified memories.

## 17. Provider architecture

Use:

```kotlin
interface ScriptGenerator {
    suspend fun generate(
        request: ScriptRequest,
        context: KenzaContext,
    ): Result<ScriptResult>
}
```

Initial providers may include:

- `DemoScriptGenerator`: deterministic samples with no network or cost
- `GeminiScriptGenerator`: uses the existing user-supplied Gemini key
- `RemoteScriptGenerator`: future secure backend that may use Claude or another model

Do not embed an Anthropic key in a public APK.

Do not add a new paid provider without explicit approval.

All network generation must support:

- Cancellation
- Timeouts
- Actionable errors
- Retry only when safe
- No full private prompt logging
- No duplicate simultaneous requests
- Draft preservation after failure

## 18. Local storage

Persist script projects across app restarts.

Each project should store:

- Stable ID
- Title
- Created and updated timestamps
- Request fields
- Generated result
- User edits
- Provider used
- Generation status
- Memory IDs used
- Candidate memories
- TTS segments
- Playback position where practical

Use local storage that matches current repository complexity. A simple bounded JSON store may be acceptable for the MVP. Use Room only when querying, migration, or data volume justifies it.

Provide deletion with confirmation.

Do not store provider keys inside script records.

## 19. Script editing

The editor should support:

- Full-text editing
- Segment navigation
- Regenerate selected segment
- Undo or restore the last generated version where practical
- Copy clean TTS text
- View production notes separately from spoken text
- Show duration estimates after edits
- Show validation warnings
- Preserve user edits during provider or playback failures

Do not automatically overwrite user edits after regeneration.

## 20. Segmentation

Long scripts may exceed provider text limits or become hard to edit.

Segment by conversational continuity, not fixed character counts alone.

A segment should:

- Begin at a natural topic or emotional transition
- End after a complete Kenza response
- Not split a pause from the reaction that follows it
- Preserve pronunciation and tone notes
- Include enough local context for consistent TTS delivery

The clean export may concatenate segments while the provider renderer processes them separately.

## 21. TTS playback and export

Support at least:

- Copy clean TTS text
- Export plain text
- Export structured JSON metadata
- Play configured TTS audio when available
- Pause, resume, seek, and stop
- Cancel rendering
- Recover from provider errors
- Clean up playback resources

When the provider reads bracketed directions aloud, transform them into supported pauses or remove non-spoken directions in the clean TTS version.

Do not assume universal SSML support.

Before rendering or sharing, show a clear reminder that the audio is synthetic and requires consent for any represented real person's voice or identity.

## 22. UI states

Script generation must model explicit states such as:

- Idle
- Validating
- Ready
- Generating
- Generated
- Editing
- Saving
- Rendering
- Playing
- Paused
- Failed
- Cancelled

Prevent duplicate generation or rendering sessions.

A failure must not erase the user's request or draft.

## 23. Demo mode

Without a provider key, the user should still be able to:

- Open Script Studio
- Browse sample projects
- Create a request
- Validate request fields
- Generate a clearly marked demo script
- Edit the script
- See duration estimation
- Save and reopen drafts
- Copy or export text
- Explore the playback interface without claiming live TTS exists

Demo mode must not imply that paid or configured generation occurred.

## 24. Accessibility and responsiveness

Validate:

- Screen-reader labels
- Logical focus order
- Font scaling
- Large touch targets
- Sufficient contrast
- Small screens
- Large screens
- Long topic names
- Long contact names
- Keyboard behavior in the editor
- Saving state during configuration changes

## 25. Tests

Add unit tests for:

- Duration bounds
- Request validation
- Prompt sections
- Memory selection limits
- One-sided dialogue warnings
- Pause parsing
- Speech and total duration estimation
- Script segmentation
- Storage serialization
- Draft preservation
- Candidate memory review behavior
- Provider error mapping
- Cancellation

Add Compose tests where the repository supports them for:

- Opening Script Studio
- Creating a request
- Validation errors
- Generation loading state
- Editing and saving
- Reopening a saved draft
- Deleting a project
- Demo mode

Provider tests should use fakes and fixtures. Paid API calls must remain manual integration tests.

## 26. Acceptance criteria for MVP

The Script Studio MVP is complete when:

- It is accessible in the APK.
- It supports 10 to 45 minute structured requests.
- It uses shared Kenza context and relevant memory.
- It generates or loads a one-sided Kenza script.
- It includes varied listener pauses.
- It estimates speaking and total duration.
- It flags obvious listener dialogue or speaker labels.
- The user can edit and save the script.
- Drafts survive restart.
- The user can copy clean TTS text.
- Demo mode works without network or paid usage.
- Provider errors preserve the draft.
- Consent and synthetic-voice disclosure remain visible.
- Unit tests pass.
- GitHub Actions produces the APK artifact.

## 27. Later acceptance criteria

The full feature may later add:

- Regenerate selected segment
- Memory review, pinning, correction, and deletion UI
- TTS rendering and playback
- Long-script audio segmentation
- Exported audio package
- Secure Claude-backed generation through a backend
- Pronunciation dictionaries
- Version history
- Search and filters
- Script templates
- Converting a script outline into a live-call conversation goal

Do not claim these are complete before implementation and validation.

## 28. Required production output shape

When a provider returns a complete script, normalize it into structured fields equivalent to:

```text
CALL TITLE:
ESTIMATED TOTAL DURATION:
ESTIMATED KENZA SPEAKING TIME:
LANGUAGE:
CALL MODE:
KENZA'S STARTING MOOD:
CORE TOPICS:
MEMORIES USED:
TTS FORMAT:

BEGIN TTS SCRIPT

[Only Kenza's audible side]

END TTS SCRIPT

CONTINUITY NOTES:
MEMORIES REFERENCED:
NEW POTENTIAL MEMORIES:
UNRESOLVED TOPICS FOR A FUTURE CALL:
PRONUNCIATION NOTES:
```

The app should display metadata in native UI rather than forcing the user to edit all headings inside one text block.

## 29. Final quality checklist

Before treating a generated script as ready:

- Only Kenza's side is present.
- The listener has enough response time.
- Kenza reacts instead of narrating full invisible replies.
- Duration approximately matches the request.
- Multiple topics flow naturally.
- Emotional tone changes gradually.
- Kenza has independent thoughts and experiences.
- Memories are used accurately.
- No major personal facts were invented.
- Language sounds spoken.
- Affection is not excessively repetitive.
- Pauses vary.
- The ending has a believable reason.
- Text is pronunciation-friendly.
- The script does not sound like a virtual assistant.
- The relationship behavior is healthy and non-manipulative.
- Consent and privacy boundaries are respected.
