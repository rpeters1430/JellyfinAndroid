# Video Playback & Transcoding Improvement Plan (All-Device Strategy)

## Goal
Build a predictable, data-driven playback pipeline that gives correct Direct Play / Direct Stream / Full Transcode decisions across flagship devices (Samsung S25 Ultra, Pixel 10 Pro XL) and long-tail Android devices.

## Current State (Project Review)

### What is working well
- Playback selection already combines Jellyfin `PlaybackInfo` with client validation in `EnhancedPlaybackManager`.
- Audio channel-aware capability checks already exist in `DeviceCapabilities` (including surround → stereo fallback logic).
- The app already supports audio-only transcode (video copy + audio transcode), which is the best intermediate path when full direct play fails.

### Primary gaps to fix
1. Decision logging is rich but not consistently structured for automated analysis.
2. Capability data is not modeled as an extensible profile attached to `DirectPlayCapabilities`.
3. Decision logic is distributed across methods and should be centralized while preserving current API behavior.
4. Transcoding URL policy has multiple call sites that need audit/consolidation.
5. Remote overrides exist via Firebase Remote Config but need security hardening for playback rules.

---

## Concrete Implementation Touchpoints (with planned edits)

1. **Structured decision logging (Phase 0)**
   - File: `app/src/main/java/com/rpeters/jellyfin/data/playback/EnhancedPlaybackManager.kt`
   - Touch existing `SecureLogger` decision points around:
     - playback entry and server-directed result
     - direct-play rejection reasons
     - direct-stream fallback decisions
     - transcoding URL generation failures

2. **Capability model extension (Phase 1)**
   - File: `app/src/main/java/com/rpeters/jellyfin/data/DeviceCapabilities.kt`
   - Extend `DirectPlayCapabilities` to include optional profile metadata (not a brand-new top-level model first).

3. **Decision centralization without API break (Phase 2)**
   - File: `app/src/main/java/com/rpeters/jellyfin/data/playback/EnhancedPlaybackManager.kt`
   - Keep `PlaybackResult` sealed class API, but route decision branches through one internal rules function.

4. **Transcoding call-site consolidation audit (Phase 3)**
   - Files:
     - `app/src/main/java/com/rpeters/jellyfin/data/playback/EnhancedPlaybackManager.kt`
     - `app/src/main/java/com/rpeters/jellyfin/data/repository/JellyfinRepository.kt`
     - `app/src/main/java/com/rpeters/jellyfin/data/repository/JellyfinStreamRepository.kt`
   - Audit and document all `streamRepository.getTranscodedStreamUrl()` call paths and normalize parameter policy.

5. **Remote rule delivery using existing Firebase stack (Phase 5)**
   - Files:
     - `app/src/main/java/com/rpeters/jellyfin/di/RemoteConfigModule.kt`
     - `app/src/main/java/com/rpeters/jellyfin/data/repository/RemoteConfigRepository.kt`
     - `app/src/main/java/com/rpeters/jellyfin/core/FeatureFlags.kt`
   - Reuse existing Remote Config flow; do not introduce a separate remote-rule service.

---

## Refined Phase Plan

## Phase 0 (1 week): Structured observability in current flow
- Add a `PlaybackDecisionTrace` payload emitted from current `SecureLogger` call sites.
- Keep current logic flow; add machine-readable fields (`decision`, `ruleId`, `reasonCode`, `mediaFingerprint`, `deviceClass`).
- Add structured failure taxonomy (`decoder_init_failed`, `unsupported_audio_codec`, `network_bitrate_exceeded`, etc.).

**Exit criteria**
- ≥95% sessions emit one structured decision trace + one terminal outcome event.

## Phase 1 (1–2 weeks): Extend `DirectPlayCapabilities` (not replacement)
- Add optional fields to `DirectPlayCapabilities`:
  - `deviceTier`, `maxVideoBitDepth`, `hevc10BitSupported`, `hdrTypes`, `maxAudioChannelsByCodec`.
- Populate from existing `DeviceCapabilities` probes + conservative defaults.
- Keep old fields untouched for compatibility.

**Exit criteria**
- All decision paths read from extended `DirectPlayCapabilities` object.

## Phase 2 (2 weeks): Centralize rules, keep `PlaybackResult` API
- Introduce one internal decision method (e.g., `computePlaybackDecision(...)`).
- Preserve public return type (`PlaybackResult`) and existing entry points.
- Ensure one reason-code list is attached before conversion to `PlaybackResult`.

**Exit criteria**
- Same inputs always yield same rule trace and same `PlaybackResult`.

## Phase 3 (1–2 weeks): Consolidate transcoding URL policy
- Audit all `getTranscodedStreamUrl()` call sites.
- Align policy for:
  - `AllowAudioStreamCopy`
  - `AllowVideoStreamCopy`
  - `TranscodingMaxAudioChannels`
  - subtitle handling
- Create one internal builder/mapper used by all call sites.

**Exit criteria**
- No conflicting transcoding parameter generation paths remain.

## Phase 4 (2 weeks): Certification matrix (flagship + mid-range early)
- Priority devices first:
  - Samsung S25 Ultra
  - Pixel 10 Pro XL
- Add mid-range early (not later):
  - Samsung A-series recent model
  - Pixel a-series recent model
  - One common Snapdragon 7xx / Dimensity class device
- Validate codec/container/audio-channel/subtitle matrix on each class.

**Exit criteria**
- Dashboard reports pass rates by flagship + mid-range cohorts.

## Phase 5 (ongoing): Firebase Remote Config rollout
- Use existing Firebase Remote Config keys/flags for playback-rule overrides.
- Add staged rollout and kill switches.
- No custom remote override backend unless Firebase limits are proven.

**Exit criteria**
- Rule rollback is possible within minutes via Remote Config.

---

## Example Decision Traces (Before/After)

### Scenario A: EAC3 5.1 on stereo-only device
**Before (current, unstructured log style)**
- `No decoder found for eac3 with 6 channels`
- `Found stereo decoder for eac3 ...`
- Outcome can be inferred but not machine-grouped.

**After (structured trace)**
```json
{
  "event": "playback_decision",
  "decision": "DIRECT_STREAM_VIDEO_COPY_AUDIO_TRANSCODE",
  "ruleId": "AUDIO_SURROUND_DOWNMIX_RULE",
  "reasonCodes": ["AUDIO_CODEC_SUPPORTED_STEREO_ONLY", "VIDEO_CODEC_DIRECT_OK"],
  "audio": {"sourceCodec": "eac3", "sourceChannels": 6, "targetCodec": "aac", "targetChannels": 2},
  "video": {"strategy": "copy"},
  "deviceClass": "samsung_flagship",
  "serverVersion": "10.x"
}
```

### Scenario B: 4K HEVC 10-bit on unsupported mid-range device
**Before**
- Mixed logs across capability check + network check; fallback path is harder to aggregate.

**After**
```json
{
  "event": "playback_decision",
  "decision": "FULL_TRANSCODE",
  "ruleId": "VIDEO_PROFILE_UNSUPPORTED_RULE",
  "reasonCodes": ["HEVC_10BIT_UNSUPPORTED", "MAX_RESOLUTION_EXCEEDED"],
  "video": {"sourceCodec": "hevc", "sourceBitDepth": 10, "targetCodec": "h264", "targetResolution": "1920x1080"},
  "audio": {"targetCodec": "aac", "targetChannels": 2}
}
```

---

## Telemetry Event Schema (PII-safe)

### Event: `playback_decision`
- `sessionId` (random UUID)
- `decision` (`DIRECT_PLAY` | `DIRECT_STREAM_VIDEO_COPY_AUDIO_TRANSCODE` | `FULL_TRANSCODE`)
- `ruleId` (string)
- `reasonCodes` (string[])
- `deviceClass` (cohort string, not exact device name)
- `mediaFingerprint` (hashed media technical signature only: codec/container/bitrate buckets)
- `networkClass` (`wifi`, `cellular`, `ethernet`, `other`)

### Event: `playback_outcome`
- `sessionId`
- `startupMs`
- `firstFailureCategory` (nullable)
- `rebufferCount`
- `fallbackUsed` (boolean)

### Event: `playback_fallback_transition`
- `sessionId`
- `fromDecision`
- `toDecision`
- `triggerReasonCode`

**Privacy guardrails**
- Never send media titles, file paths, user IDs, subtitles text, or watch-history content.
- Keep using existing SecureLogger/PII-safe principles for local logging.

---

## Security Considerations

1. **Remote override integrity**
   - Playback override payloads must be cryptographically signed.
   - App validates signature before applying override values.
   - Reject/ignore unsigned or invalid payloads and fall back to safe defaults.

2. **Safe rollout controls**
   - Staged rollout + kill switch through Firebase Remote Config.
   - Version pinning to prevent stale/rollback confusion.

3. **Telemetry minimization**
   - Technical-only telemetry fields.
   - No personal viewing behavior or content identity leakage.

---

## Practical rules to apply immediately
1. Prefer Direct Stream (video copy + audio transcode) over full transcode when video is supported.
2. When surround is stereo-only on device, force explicit downmix/transcode policy.
3. Never up-transcode beyond source bitrate/resolution.
4. Keep universal fallback ladder: `h264 + aac + ts/mp4`.
5. Every fallback transition must emit machine-readable reason codes.

## Suggested backlog tickets
1. Add `PlaybackDecisionTrace` + structured logger helper in `EnhancedPlaybackManager`.
2. Extend `DirectPlayCapabilities` with optional profile metadata fields.
3. Add internal decision engine method while preserving `PlaybackResult` return types.
4. Audit/consolidate all `getTranscodedStreamUrl()` call sites.
5. Add flagship + mid-range certification matrix automation.
6. Add Firebase Remote Config playback rule flags with signature validation.
7. Add PII-safe telemetry events + dashboards.
