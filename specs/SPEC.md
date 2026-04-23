# pen-pal: Voice-First Story Organizer

## Project Overview

**pen-pal** is a voice-first story organization app that helps writers capture ideas through voice recordings, transcribe them on-device, and visualize the narrative flow through an interactive knowledge graph.

### Core Vision
- Record voice notes about your story
- AI transcribes and generates summaries automatically
- Visual graph shows how scenes and characters connect
- Fully offline, free and open source

### Target Users
- Primary: Personal use for story writers
- Community: Open source for anyone who finds it useful

---

## Features

### 1. Voice Recording
- Record voice memos directly in the app
- Real-time waveform visualization
- Playback with ExoPlayer
- Save as internal audio files

### 2. On-Device Transcription
- Whisper.cpp for offline transcription
- Segment-based processing
- Timestamp support for scene segmentation

### 3. Auto Summary + Title
- If TinyLlama present: AI-generated title and summary
- Else: Segment-based title from first sentence + timestamp grouping

### 4. Knowledge Graph

#### Graph Structure
```
Scene 1 ◄──────► Scene 2 ◄──────► Scene 3 (horizontal timeline)
   │               │               │
   ▼               ▼               ▼
Char A, B ◄──► Char A, C ◄──► Char B, C (vertical connections)
```

#### Node Interactions
- **Click Scene Node** → Shows summary, characters, location, mood, full transcript
- **Click Character Node** → Shows horizontal journey of ALL scenes that character appears in

#### Visual Layout
- Timeline-first: scenes flow horizontally (chronologically)
- Scene details: characters connect vertically to their scenes
- User can drag any node to reposition
- Pinch to zoom, pan to navigate

### 5. Character Management
- Auto-extraction from transcribed text
- Multiple characters per scene supported
- User correction: merge/split/rename characters
- Character detail view with all scenes summary

### 6. Export Options
- Plain text (.txt) — readable story format
- JSON — backup/import capability
- Timeline image (PNG) — graph visualization

---

## Technical Specification

### Tech Stack

| Component | Technology |
|-----------|------------|
| Platform | Android |
| Language | Kotlin 1.9.x |
| UI | Jetpack Compose (BOM 2024.02.x) |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Storage | Room (SQLite) |
| Audio | AudioRecord + ExoPlayer |
| Transcription | Whisper.cpp (JNI) |
| AI (optional) | TinyLlama or Transformers.js |
| Graph | Custom Compose Canvas |

### Architecture

```
pen-pal/
├── src/main/java/com/penpal/
│   ├── di/                      # Hilt modules
│   │
│   ├── data/                   # Data Layer
│   │   ├── local/              # Room database
│   │   │   ├── dao/
│   │   │   ├── entity/
│   │   │   └── database/
│   │   ├── repository/         # Repository implementations
│   │   └── model/              # Data models
│   │
│   ├── domain/                 # Domain Layer
│   │   ├── model/              # Domain entities
│   │   ├── repository/        # Repository interfaces
│   │   └── usecase/            # Use cases
│   │
│   ├── presentation/           # Presentation Layer
│   │   ├── ui/
│   │   │   ├── screens/        # Screen composables
│   │   │   ├── components/     # Reusable UI components
│   │   │   ├── navigation/     # Compose Navigation
│   │   │   └── theme/          # Material 3 theme
│   │   ├── viewmodel/         # ViewModels
│   │   └── model/             # UI state models
│   │
│   ├── transcription/          # Transcription (JNI)
│   │   ├── jni/                # C++ JNI bindings
│   │   ├── model/              # Whisper model loader
│   │   └── pipeline/          # Recording → Transcription pipeline
│   │
│   ├── graph/                   # Knowledge Graph
│   │   ├── data/              # Graph data structures
│   │   ├── renderer/          # Canvas rendering
│   │   ├── interaction/       # Gesture handling
│   │   └── context/           # Narrative context engine
│   │
│   └── ai/                     # AI Processing
│       ├── summary/           # Title + summary generation
│       ├── entity/             # Character/scene extraction
│       └── context/            # Narrative state machine
│
└── assets/
    └── models/                 # Whisper + TinyLlama models
```

### Room Database Schema

```kotlin
// Stories (projects)
@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long
)

// Voice recordings
@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey val id: String,
    val storyId: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long
)

// Scenes (transcribed segments)
@Entity(tableName = "scenes")
data class Scene(
    @PrimaryKey val id: String,
    val storyId: String,
    val recordingId: String,
    val title: String,
    val content: String,           // transcribed text
    val summary: String?,           // AI-generated summary
    val location: String?,         // where scene takes place
    val mood: String?,             // emotional tone
    val orderIndex: Int,           // position in timeline
    val timestamps: String,        // JSON: {start: ms, end: ms}
    val createdAt: Long
)

// Characters
@Entity(tableName = "characters")
data class Character(
    @PrimaryKey val id: String,
    val storyId: String,
    val name: String,
    val aliases: String,           // JSON: ["John", "Mr. Smith"]
    val description: String?,
    val color: Int,                 // hex color for graph
    val createdAt: Long
)

// Scene-Character connections
@Entity(tableName = "scene_characters")
data class SceneCharacter(
    @PrimaryKey val id: String,
    val sceneId: String,
    val characterId: String
)

// Graph node positions
@Entity(tableName = "graph_nodes")
data class GraphNode(
    @PrimaryKey val id: String,
    val type: String,               // "scene" or "character"
    val x: Float,
    val y: Float,
    val updatedAt: Long
)
```

---

## Development Phases

### Phase 1: Project Foundation (Week 1)
- [ ] Project setup (Kotlin, Compose, Hilt)
- [ ] Clean Architecture skeleton
- [ ] Room database + entities
- [ ] Navigation setup
- [ ] Material 3 theme
- [ ] Shell build verification

### Phase 2: Voice Recording (Week 2)
- [ ] Audio recording with AudioRecord
- [ ] Waveform visualization
- [ ] Recording list UI
- [ ] Playback with ExoPlayer

### Phase 3: Transcription Engine (Week 3)
- [ ] Compile whisper.cpp for Android ARM64
- [ ] JNI bindings (Kotlin ↔ C++)
- [ ] Model loading/downloading
- [ ] Transcription pipeline

### Phase 4: AI Processing (Week 4)
- [ ] Summary + title generation
- [ ] Entity extraction
- [ ] Character coreference

### Phase 5: Knowledge Graph (Week 5)
- [ ] Graph data structures
- [ ] Canvas rendering (nodes, edges, timeline)
- [ ] Gesture handling (pan, zoom, drag)
- [ ] Scene-Character connections

### Phase 6: Integration & Polish (Week 6)
- [ ] Link recordings → scenes → graph
- [ ] Character journey view
- [ ] User correction UI
- [ ] Export functionality

### Phase 7: Build & Release (Week 7)
- [ ] Debug APK build
- [ ] Testing
- [ ] Push to GitHub

---

## Success Criteria

- [ ] User can record voice and see waveform
- [ ] Recordings transcribe to text on-device
- [ ] Summary and title auto-generated
- [ ] Knowledge graph shows scene connections
- [ ] Clicking scene shows details
- [ ] Clicking character shows all their scenes
- [ ] User can manually correct any node
- [ ] Export to text/JSON works
- [ ] All on-device, works offline
- [ ] Public on GitHub

---

## Dependencies

### Gradle (Kotlin DSL)
```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// Hilt
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-android-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// ExoPlayer
implementation("androidx.media3:media3-exoplayer:1.2.1")
implementation("androidx.media3:media3-ui:1.2.1")

// Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
```

---

## License

MIT License — free for anyone to use, modify, and distribute.