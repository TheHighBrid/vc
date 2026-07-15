# Call — iOS-style call screen with a live AI voice (Android)

A native Android app that recreates the iPhone call experience — incoming-call
ring screen, in-call control grid, and a working dial pad — and connects the
call to a **live, two-way AI voice conversation** in the cloned voice.

The voice + brain are handled by an **ElevenLabs Conversational AI** agent:

```
  mic ──PCM──▶  ElevenLabs agent  ──PCM──▶  speaker
                 ├─ speech-to-text (your words)
                 ├─ LLM brain  (pick GPT or Gemini in the dashboard)
                 └─ text-to-speech in voice cloned voice
```

The Android app stays thin: it streams microphone audio up over a WebSocket and
plays the agent's audio back, so replies feel live and in-context.

---

## ⚠️ Consent (read this first)

Cloning a real person's voice requires **that person's explicit consent**.
ElevenLabs' terms require you to confirm you have permission to clone any voice
you upload. Only use the person's voice with her clear, informed agreement, and don't
use the app to deceive or impersonate her to others.

---

## What you get

- **Incoming call screen** — avatar, name, ringtone + vibration, Decline / Accept.
- **In-call screen** — duration timer, mute, speaker, keypad (DTMF tones),
  and the iOS control grid (add call / FaceTime / contacts are visual).
- **Home dial pad** — iOS keypad with letters, green call button, backspace,
  and a "Simulate incoming call" button to test the ring flow.
- **Live voice** — real-time STT → LLM → cloned-voice TTS via ElevenLabs,
  with barge-in (you can interrupt and she stops talking).
- **Demo mode** — with no agent configured, every screen still works; there's
  just no live voice. Great for trying the UI first.

---

## Prerequisites

- **Android Studio** (Koala/Ladybug or newer) with an Android SDK.
- A device or emulator running **Android 8.0 (API 26)** or higher.
  > Use a **physical device** for the real voice call — emulator mic/audio
  > routing is unreliable.
- An **ElevenLabs** account (Creator plan or higher for voice cloning).

---

## 1. Clone the voice in ElevenLabs

1. Record a clean voice sample of vvoice (1–3 minutes, quiet room). With her consent.
2. In the ElevenLabs dashboard → **Voices → Add Voice → Instant Voice Clone**
   (or **Professional Voice Clone** for higher realism), upload the sample.
3. Note the created **voice**.

## 2. Create the Conversational AI agent

1. Dashboard → **Conversational AI → Create Agent**.
2. **Voice:** select a cloned voice.
3. **LLM:** choose your brain — e.g. GPT-4o-mini / GPT-4o, or Gemini.
4. **System prompt:** give it her persona, e.g.
   > "You are ..., warm and playful. Keep replies short and natural, like a
   > real phone call. Speak in first person."
5. **Audio output format:** set to **PCM 16000 Hz** (the app expects 16 kHz PCM).
6. Save and copy the **Agent ID**.
7. Auth: keep the agent **public** for the simplest setup. If you make it
   **private**, you'll also need an **API key** (next step).

## 3. Add your keys

```bash
cp local.properties.example local.properties
```

Edit `local.properties`:

```properties
ELEVENLABS_AGENT_ID=your_agent_id_here
# only for a PRIVATE agent:
# ELEVENLABS_API_KEY=your_api_key_here
CONTACT_NAME=Luna
```

`local.properties` is git-ignored, so keys never get committed. They're injected
into `BuildConfig` at build time.

## 4. Build & run

- **Android Studio:** open this folder, let Gradle sync, pick your device, Run ▶.
- **Command line:**
  ```bash
  ./gradlew installDebug
  ```

On first launch, grant the **microphone** permission when prompted (needed once
you start a real call).

---

## How to use it

- **Outgoing:** type any number (it's a simulation) → tap the green call button.
- **Incoming:** tap **Simulate incoming call** → hear the ring → **Accept**.
- Once connected, just talk — Luna replies live. Use **mute** / **speaker** /
  **keypad** as on a real iPhone; the red button ends the call.

---

## Project layout

```
app/src/main/java/com/Luna/callsim/
├─ MainActivity.kt            # hosts Compose UI, requests mic permission
├─ call/
│  ├─ CallViewModel.kt        # call state machine + audio routing + session
│  ├─ CallState.kt            # UI state model
│  └─ SoundFx.kt              # ringtone/vibration + DTMF keypad tones
├─ voice/
│  ├─ AudioIo.kt              # MicRecorder (16 kHz PCM) + PcmPlayer
│  └─ ConversationalAiClient.kt  # ElevenLabs WebSocket protocol
└─ ui/
   ├─ CallApp.kt              # switches screens by call phase
   ├─ components/Keypad.kt    # iOS dial pad + round control button
   └─ screens/                # Home, IncomingCall, InCall
```

---

## Customizing

- **Ringtone:** drop `ringtone.mp3` (or `.ogg`) into `app/src/main/res/raw/`;
  it's used automatically. Otherwise the system default ringtone plays.
- **Her persona / how she talks:** edit the agent's system prompt in ElevenLabs.
- **Different brain:** switch the agent's LLM (GPT ↔ Gemini) in the dashboard —
  no app changes needed.

---

## Cost & latency notes

- ElevenLabs Conversational AI bills per minute of conversation; check current
  pricing on their site. The LLM and TTS usage are included in that.
- For lowest latency use a fast LLM (e.g. GPT-4o-mini / Gemini Flash) and the
  Flash TTS model on the agent.

## Troubleshooting

- **"Demo mode" banner / no voice:** `ELEVENLABS_AGENT_ID` isn't set — recheck
  `local.properties` and re-sync Gradle.
- **Connects then drops:** if the agent is private, set `ELEVENLABS_API_KEY`;
  also confirm the agent's output format is **PCM 16000**.
- **Robotic/garbled audio:** the agent output format isn't 16 kHz PCM — fix it
  in the agent's voice/audio settings.
- **No sound on emulator:** test on a physical device.

## Security

Embedding an ElevenLabs API key in the APK is fine for personal use but is not
safe for public distribution (it can be extracted). For a shipped app, mint
signed URLs from a small backend instead of bundling the key.
