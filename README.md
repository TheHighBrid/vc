# Allo

Allo is a native Android call simulator that recreates the feel of an iPhone call interface and connects it to a live, two-way AI conversation with Kenza.

The app is **Gemini-first**. Gemini Live is the recommended everyday engine because it supports low-latency voice calls on a free tier and powers Allo's automatic post-call memory system. ElevenLabs remains available as an optional paid compatibility provider, but it is not required for normal use.

```text
microphone ──PCM──▶ Gemini Live ──PCM──▶ speaker
                     ├─ live speech understanding
                     ├─ conversational reasoning
                     ├─ spoken response generation
                     └─ Kenza memory + personality briefing
```

## Consent and private use

Allo simulates a private phone conversation with an AI character. Only use a real person's voice, identity, recordings, or private history with that person's clear and informed consent. Do not use the app to deceive, impersonate, authenticate as, or misrepresent another person.

## What Allo includes

- **Incoming call screen** with avatar, ringtone, vibration, Decline, and Accept
- **Active call screen** with timer, mute, speaker, keypad, and iOS-style controls
- **Home dial pad** with DTMF tones and incoming-call simulation
- **Scheduled calls** that can ring as incoming calls
- **Gemini Live voice** for the recommended everyday call experience
- **Barge-in** so the user can interrupt while Kenza is speaking
- **Automatic reconnection** for recoverable network or session interruptions
- **Encrypted Kenza memory** stored privately on the Android device
- **Post-call summaries** with mood, important events, plans, preferences, and follow-ups
- **Editable personality profiles** for Kenza, Mohamed, their relationship, goals, and boundaries
- **Memory review screen** for pinning, completing, deleting, and manually adding memories
- **Demo mode** when no live provider is configured
- **Optional ElevenLabs support** for users who deliberately want a paid cloned-voice provider

## Recommended setup: Gemini

### 1. Requirements

- Android Studio Koala/Ladybug or newer
- Android SDK with Java 17
- Android 8.0 / API 26 or newer
- A physical Android device for reliable microphone and speaker testing
- A Gemini API key from Google AI Studio

### 2. Configure Gemini

The easiest method is directly inside Allo:

1. Open **Settings**.
2. Select **Gemini · Daily**.
3. Paste the Gemini API key.
4. Choose a voice.
5. Keep the default low-latency model unless testing another supported Gemini Live model.
6. Save.

You can also configure the build locally:

```bash
cp local.properties.example local.properties
```

Then add:

```properties
GEMINI_API_KEY=your_key_here
CONTACT_NAME=Kenza
```

`local.properties` is ignored by Git, so local secrets are not committed.

### 3. Build and install

```bash
./gradlew installDebug
```

Or open the repository in Android Studio, allow Gradle to sync, choose a physical device, and press Run.

On first launch, grant microphone permission.

## Kenza memory system

Allo automatically loads Kenza's personality and relationship memory before every outgoing, incoming, or scheduled call.

The private pre-call briefing can include:

- Kenza's personality and background
- Mohamed's profile
- Shared relationship context
- Ambitions and long-term goals
- Important boundaries
- Recent call summaries
- Important facts and preferences
- Shared memories
- Open plans
- Upcoming commitments
- Unresolved conversations
- Relevant follow-up topics
- Current date, time, season, and normal call rhythm

Kenza is instructed to use memory naturally, not recite it or claim memories that are not stored.

### After a call

When a real two-way call ends, Allo temporarily uses the transcript to create:

- A compact call summary
- The emotional mood
- Important highlights
- Durable facts and preferences
- Shared relationship memories
- Plans and goals
- Corrections
- Unresolved topics
- A useful follow-up for the next call

The raw transcript is then discarded. It is not retained as permanent memory.

### Storage and privacy

Memory is stored in an app-private encrypted file using AES-GCM with a key protected by Android Keystore.

- Memory is not committed to GitHub
- Raw transcripts are not permanently stored
- Other ordinary apps cannot read the memory file
- Existing older-format memories are migrated automatically
- Memories can be reviewed and deleted inside the app
- Uninstalling the app removes its local memory unless a future export/backup feature is used

## Memory screen

Open **Memory** from the home dialer to manage:

- Kenza's profile
- Mohamed's profile
- Relationship history
- Ambitions and future goals
- Boundaries and important context
- Recent call summaries
- Remembered facts and preferences
- Open plans
- Completed plans
- Pinned memories
- Manual memories

## Everyday use

- **Outgoing call:** enter any simulated number and tap the green call button
- **Incoming call:** tap **Simulate incoming call**, then Accept
- **Scheduled call:** create a schedule and allow notification permissions
- **During the call:** speak normally, interrupt naturally, use mute/speaker/keypad, and tap the red button to end
- **After the call:** open Memory to review the generated summary and important points

## Optional ElevenLabs provider

ElevenLabs is retained only for versatility and compatibility. It is paid, quota-limited, and not the recommended daily engine.

To test it intentionally:

1. Create an ElevenLabs Conversational AI agent with proper consent.
2. Add its Agent ID in Allo Settings.
3. Add an API key only if the agent is private.
4. Enable prompt overrides in the ElevenLabs agent before turning on persona and memory injection.

Gemini remains useful even when ElevenLabs is selected because Gemini performs post-call memory extraction when a Gemini key is configured.

Do not rely on ElevenLabs backup accounts as a normal daily-use strategy.

## Project layout

```text
app/src/main/java/com/kenza/callsim/
├─ MainActivity.kt
├─ call/
│  ├─ CallViewModel.kt
│  ├─ CallState.kt
│  └─ SoundFx.kt
├─ config/
│  └─ ConfigRepository.kt
├─ memory/
│  ├─ MemoryModels.kt
│  ├─ MemoryStore.kt
│  ├─ SecureMemoryStorage.kt
│  ├─ MemoryExtractor.kt
│  └─ MemoryContext.kt
├─ schedule/
├─ voice/
│  ├─ AudioIo.kt
│  ├─ GeminiLiveProvider.kt
│  ├─ ElevenLabsProvider.kt
│  └─ VoiceProvider.kt
└─ ui/
   ├─ CallApp.kt
   ├─ components/
   └─ screens/
      ├─ HomeScreen.kt
      ├─ IncomingCallScreen.kt
      ├─ InCallScreen.kt
      ├─ MemoryScreen.kt
      ├─ ScheduleScreen.kt
      └─ SettingsScreen.kt
```

## Customization

- **Persona:** edit the Personality field in Settings
- **Long-term profiles:** edit the sections inside Memory
- **Voice:** choose a Gemini voice in Settings
- **Ringtone:** place `ringtone.mp3` or `ringtone.ogg` in `app/src/main/res/raw/`
- **Contact name:** change it in Settings or `local.properties`

## Testing

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

GitHub Actions runs unit tests and builds debug and release APK artifacts for pushes and pull requests affecting Android code.

## Troubleshooting

### Demo mode or no live voice

Open Settings, select **Gemini · Daily**, and add a valid Gemini API key.

### Call connects but voice is silent

- Test on a physical device
- Confirm microphone permission
- Confirm media volume
- Verify the selected Gemini Live model is currently supported
- Check that another app is not holding the microphone

### Post-call memory was not created

- Confirm a Gemini API key is saved
- Confirm both the user and Kenza spoke during the call
- Open Memory and check whether the summary is still processing
- Verify internet access after hang-up

### ElevenLabs fails

This does not affect normal Gemini operation. ElevenLabs is optional. Check its Agent ID, private-agent key, quota, and prompt-override permissions only when deliberately testing that provider.

## Security

Never commit API keys, voice recordings, private memories, call transcripts, or relationship data. Public production builds should avoid embedding long-lived provider credentials in the APK.
