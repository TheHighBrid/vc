# 🚀 Launching Kenza Call on Google Play — your step-by-step guide

This document is the single place that tells you exactly what is **already done**
and the few things **only you can do** (because they need your accounts, your
money, your phone, or your identity).

---

## ✅ What I already did for you

- Made the app **release-ready**: target SDK 35 (Play requirement), R8 code
  shrinking + resource shrinking, `BuildConfig` wired to your config.
- Added an **in-app Settings screen** so the ElevenLabs Agent ID can be entered
  on the phone — **no rebuild needed** when you get it.
- Added a first-run **consent disclaimer** dialog (voice-cloning policy).
- Generated your **upload signing keystore** (`upload-keystore.jks`) and
  `keystore.properties`, and wired release signing to it.
- Built a **signed release App Bundle (`.aab`)** and a signed **APK** — see the
  files I sent you in chat.
- Wrote your **privacy policy** (`docs/privacy-policy.html`), **store listing
  text** (`docs/store-listing.md`), and the **Data safety** + **content rating**
  answers (below).
- Wired your Voice ID `eXpIbVcVbLo8ZJQDlDnl` into the build config.

> ⚠️ **Keep these two files safe and private** (I deliberately kept them OUT of
> git): `upload-keystore.jks` and `keystore.properties`. If you lose the
> keystore you can reset the upload key via Play, but back it up anyway. The
> keystore password is in the chat message where I generated it.

---

## 🟡 What only you can do

### Step 1 — Create the ElevenLabs agent (gives the live voice) · ~5 min
1. Sign in at **https://elevenlabs.io**.
2. Confirm Kenza's cloned voice exists (Voice ID `eXpIbVcVbLo8ZJQDlDnl`). If you
   only have a sample, create the clone first (with her consent).
3. Go to **Conversational AI → Agents → Create agent**.
4. **Voice:** select Kenza's voice (`eXpIbVcVbLo8ZJQDlDnl`).
5. **LLM:** choose a fast model — GPT-4o-mini or Gemini 1.5/2.0 Flash.
6. **System prompt** (her personality), e.g.:
   > "You are Kenza, warm, playful and caring. Keep replies short and natural,
   > like a real phone call. Speak in the first person."
7. **Audio output format:** set to **PCM 16000 Hz** (important — the app expects this).
8. **Authentication:** keep the agent **Public** (simplest; no API key needed in
   the app). Only make it Private if you understand you'd then need a backend to
   keep the key secret.
9. Copy the **Agent ID**.

### Step 2 — Put the Agent ID into the app · ~1 min
Two options:
- **Easiest (no rebuild):** install the app, open **Settings** (gear icon, top
  right of the dial screen), paste the **Agent ID**, tap **Save**. Done — live
  voice works.
- **Bake it into the build:** add to `local.properties`:
  ```
  ELEVENLABS_AGENT_ID=your_agent_id_here
  ```
  then rebuild (Step 6). Use this if you want the shipped app pre-configured.

### Step 3 — Host the privacy policy (Play requires a public URL) · ~3 min
Your policy file is at `docs/privacy-policy.html`. Publish it free with GitHub Pages:
1. Push is already done. Go to your repo on GitHub → **Settings → Pages**.
2. Under **Build and deployment → Source**, pick **Deploy from a branch**.
3. Branch: `claude/ios-call-screen-android-ai-37ri4t` (or merge to `main` first
   and pick `main`), folder: **/docs**. Save.
4. After a minute your URL is:
   `https://<your-github-username>.github.io/vc/privacy-policy.html`
5. Open it to confirm it loads. You'll paste this URL into Play.

### Step 4 — Create your Google Play Developer account · ~15 min + $25
1. Go to **https://play.google.com/console** and sign in with the Google account
   you want to own the app (your `wabazze@gmail.com` works).
2. Pay the **one-time $25** registration fee.
3. Complete **identity verification** (Google may take 1–2 days to verify a new
   personal developer account). You cannot publish until this clears.

### Step 5 — Create the app in Play Console & fill it in · ~30 min
1. **Create app:** name `Kenza Call`, language English (US), type **App**, **Free**.
2. **Store listing:** copy text from `docs/store-listing.md` (short + full
   description). Upload graphics (icon 512², feature graphic 1024×500, ≥2
   screenshots — take them on your phone). Set category **Communication**.
3. **Privacy policy:** paste the GitHub Pages URL from Step 3.
4. **App access:** since the app needs an agent to do the live call, choose
   "All functionality is available without special access" if you ship it
   pre-configured, OR provide test instructions: *"Open Settings, paste the
   provided ElevenLabs Agent ID, then place a call."* (Provide a test Agent ID
   to reviewers.)
5. **Ads:** No ads.
6. **Content rating:** fill the questionnaire — see answers below.
7. **Data safety:** fill the form — see answers below.
8. **Target audience:** 18+ (recommended for an AI-voice companion app).
9. **Government apps / financial / health:** No.

### Step 6 — (Only if you rebuild) build the signed release yourself
Everything is set up; from the project root:
```bash
# signed App Bundle for Play:
./gradlew bundleRelease
# output: app/build/outputs/bundle/release/app-release.aab

# signed APK (for sideloading/testing):
./gradlew assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```
(Needs Android Studio / SDK installed locally. The keystore is already wired.)

### Step 7 — Upload & roll out · ~15 min
1. Start with **Testing → Internal testing** (fastest review, ideal for a
   personal app). Create a release, **upload the `.aab`** I gave you.
2. Add your own email as a tester; share the opt-in link; install from Play.
3. When happy, promote to **Production** (or stay on a testing/closed track if
   this is just for the two of you — recommended, see the note below).

---

## 📋 Data safety form — use these answers

- **Does your app collect or share user data?** Yes (audio is sent to a third party).
- **Data types collected:**
  - **Audio → Voice or sound recordings.**
    - Collected: **Yes** · Shared: **Yes** (with ElevenLabs)
    - Processed **ephemerally**? **Yes** (not stored by your app)
    - Required or optional: **Required** (core feature)
    - Purpose: **App functionality**
- **Other data types** (location, contacts, financial, identifiers, etc.): **No.**
- **Is all collected data encrypted in transit?** **Yes** (HTTPS/WSS).
- **Can users request data deletion?** Provide the contact email; on-device data
  is removed on uninstall.

## 📋 Content rating questionnaire — typical answers

- Category: **Utility / Communication.**
- Violence, sexual content, profanity, controlled substances, gambling: **No.**
- **User-generated / user-to-user communication:** the app sends your voice to an
  AI; there is no communication with other users → answer **No** to user-to-user
  features. (If asked about user-generated content shared with others: **No.**)
- Result: should come out **Everyone / PEGI 3–style**, though you may set the
  **target age to 18+** in the audience section.

---

## ⚠️ Important honest caveats (please read)

1. **Impersonation policy risk.** Google Play prohibits apps that facilitate
   impersonation or deception. An app described as "calls in a real person's
   voice" can attract scrutiny. **Strong recommendation:** keep this on an
   **Internal/Closed testing track for personal use** rather than a public
   Production launch. If you do go public, keep the listing framed as a
   *personal AI companion with consent* (the provided listing text does this) and
   be ready to explain the consent basis.

2. **Public agent = your ElevenLabs credits.** A public agent can be used by
   anyone who has the agent ID. For a personal app that's a minor risk; for a
   public launch, move to a backend that mints signed URLs and rotate as needed.

3. **API key in the app is not secret.** Only use a Private agent + embedded key
   for personal builds. For real distribution, use a backend.

4. **First Play review** of a new account + mic permission can take a few days.

---

## TL;DR of your remaining clicks
1. Create ElevenLabs agent → copy Agent ID. *(5 min)*
2. Paste Agent ID in the app's Settings (or `local.properties`). *(1 min)*
3. Enable GitHub Pages for `/docs` → get privacy URL. *(3 min)*
4. Pay $25, verify identity on Play Console. *(15 min + wait)*
5. Create app, paste listing text + privacy URL, upload screenshots. *(30 min)*
6. Upload the `.aab` I gave you to Internal testing, roll out. *(15 min)*

Everything else (code, signing, build, policy text, form answers) is done.
