# pen-pal

Voice-first story organization app with AI transcription and visual knowledge graph.

## Project Context

- **Owner**: suprxsidh (GitHub)
- **Repo**: pen-pal (public)
- **Platform**: Android (Kotlin + Jetpack Compose)

## Core Features

1. **Voice Recording** — Record voice memos about your story
2. **On-Device Transcription** — Whisper.cpp for offline transcription
3. **Auto Summary + Title** — Segment-based or TinyLlama
4. **Knowledge Graph** — Visual timeline of scenes and characters
5. **Character Journey** — Click character to see all their scenes
6. **User Correction** — Merge/split/rename any node

## Graph Structure

```
Scene 1 ◄──────► Scene 2 ◄──────► Scene 3 (horizontal timeline)
   │               │               │
   ▼               ▼               ▼
Char A, B ◄──► Char A, C ◄──► Char B, C (vertical connections)

Click Scene    → Summary + details
Click Character → Horizontal journey showing ALL scenes
```

## Architecture

- Clean Architecture (domain/data/presentation layers)
- MVVM with Hilt DI
- Room for local storage
- Custom Compose Canvas for graph visualization

## Key Files

- `specs/SPEC.md` — Full specification
- `future_plans.md` — Progress tracking
- `src/main/java/com/penpal/` — Source code

## Dependencies

- Kotlin 1.9.x, Compose BOM 2024.02.x
- Hilt 2.50, Room 2.6.1
- ExoPlayer (Media3 1.2.1)
- Whisper.cpp (JNI) for transcription

## Important Notes

- All on-device, works offline
- Multiple characters per scene supported
- User can drag/reposition any graph node
- Export: .txt (plain text) + JSON

## Current Status

Phase 1-3 Complete, Phase 3 Fix Applied:
- Project Foundation ✅
- Voice Recording ✅
- Transcription ✅
- Graph Screen (fixed, now implemented) ✅

All navigation routes working now.

All phases follow systematic-debugging, test-driven-development, and verification-before-completion practices.