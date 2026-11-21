# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DocMorph Notes AI is an Android application that converts documents to PDF using AI-powered features, LaTeX compilation via Termux, OCR, and various document processing capabilities. The app is built with Kotlin and Jetpack Compose, following clean architecture principles.

**Key Technologies:**
- Kotlin with Jetpack Compose for UI
- Material 3 Design with custom glass-morphism theme
- Firebase Gemini AI for text generation and processing
- Termux integration for XeLaTeX compilation
- PDFBox Android for PDF rendering
- ML Kit for OCR
- Room for local database
- DataStore for preferences
- Ktor for HTTP client
- WorkManager for background tasks

## Architecture

The codebase follows **clean architecture** with three main layers:

### Data Layer (`app/src/main/java/com/example/data/`)
- **database/**: Room entities, DAOs for projects and document snippets
- **datastore/**: Preferences storage (AI settings, model configs)
- **network/**: `AiNetworkClient` for API communications
- **cache/**: Response caching (`AiResponseCache`)
- **repository/**: Data access abstractions (e.g., `LatexChatRepository`)

### Domain Layer (`app/src/main/java/com/example/domain/`)
- **models/**: Core business models (`AiProvider`, `ChatMessage`, `DocumentModels`, etc.)
- **services/**: Business logic organized by feature:
  - `ai/`: AI services (`BlueprintService`, `NoteGenerationService`, `TextChunker`)
  - `export/`: Export logic (`ExportEngine`, `MathRenderer`)
  - `pdf/`: PDF compilation (`TermuxXeLaTeXBridge`, `TectonicBridge`, `PdfRendererService`)
  - `ocr/`: OCR processing (`LocalOcrEngine`)
  - `chat/`: Chat functionality
  - `worker/`: Background work coordination
  - `updater/`: App self-update system

### UI Layer (`app/src/main/java/com/example/ui/`)
- **screens/**: Feature-specific screens (home, blueprint, chat, processing, viewer, settings, math, debugger, updater, project)
- **components/**: Reusable UI components, including custom `glass/` components for glass-morphism effects
- **theme/**: Material 3 theming with custom font support
- **navigation/**: Navigation logic

**Entry points:**
- `MainActivity.kt`: Sets up navigation between onboarding and main screen, handles deep links with `docmorph://` scheme
- `MainScreen.kt`: Primary screen with bottom navigation (Dashboard, Home, Projects, Settings)
- `DocMorphApplication.kt`: Application class, initializes Timber logging and DataStore caching

## Termux Integration

The app has a **critical dependency on Termux** for LaTeX compilation:

- `TermuxXeLaTeXBridge.kt`: HTTP bridge to Termux server for XeLaTeX compilation
- Requires Termux permission: `com.termux.permission.RUN_COMMAND`
- Python server (`termux_server.py`) runs in Termux to handle compilation requests
- Alternative: `TectonicBridge.kt` (may be used for embedded compilation)

When working on LaTeX features, be aware of this external dependency. The bridge sends LaTeX content via HTTP to a local Termux server.

## Build & Development

### Building the App
```bash
# Build debug APK
bash gradlew assembleDebug

# Build release APK (requires keystore)
bash gradlew assembleRelease

# Install to device
bash gradlew installDebug
```

**Note:** Java/Gradle commands require proper JAVA_HOME setup in Termux environment.

### Signing Configuration
- Debug: Uses `debug.keystore` in project root
- Release: Configured via environment variables:
  - `KEYSTORE_PATH`: Path to keystore file
  - `STORE_PASSWORD`: Keystore password
  - `KEY_PASSWORD`: Key password

### Running Tests
```bash
# Unit tests (Robolectric + Roborazzi for screenshot testing)
bash gradlew test

# Instrumented tests
bash gradlew connectedAndroidTest

# Single test
bash gradlew test --tests "com.example.SpecificTest"
```

### Environment Setup
Configure `.env` file (see `.env.example`):
- `GEMINI_API_KEY`: Required for Gemini AI features

## Python Utility Scripts

The repository contains **135+ Python scripts** in the root directory for development automation:
- `fix_*.py`: Syntax and code correction scripts
- `patch_*.py`: Feature injection and modification scripts
- `test_*.py`: Testing utilities

These are development tools, not part of the runtime application. Use them when making bulk changes or fixing issues across multiple files.

## Key Features & Workflows

### Update System

The app uses GitHub Actions artifacts for self-updates. Nightly builds are published as pre-releases under the `nightly` tag. The in-app updater:
- Checks for new builds via ETag comparisons
- Downloads artifacts with progress tracking
- Validates APK signatures
- Uses Android's FileProvider for secure installation

### 1. Document Processing Pipeline
User uploads document → OCR via `LocalOcrEngine` → AI processing via `BlueprintService`/`NoteGenerationService` → Export via `ExportEngine` → PDF compilation via Termux bridge

### 2. LaTeX Compilation
LaTeX code → `TermuxXeLaTeXBridge` sends HTTP request to Termux server → XeLaTeX compiles → PDF returned → Rendered via `PdfRendererService`

### 3. AI Integration
- Uses Firebase Gemini AI (`firebase.ai` dependency)
- Settings stored in `AiSettingsDataStore`
- Responses cached in `AiResponseCache`
- Usage tracked by `AiUsageTracker`

### 4. Project Management
- Projects stored in Room database (`ProjectEntity`, `ProjectDao`)
- Document snippets associated with projects (`DocumentSnippetEntity`)

### 5. Theme System
- Material 3 with dynamic color support
- Custom glass-morphism components in `ui/components/glass/`
- Theme modes: Light, Dark, System (stored in `AiSettingsDataStore`)
- Custom font support via `customFontPath` setting

## Common Development Tasks

### Adding a New Screen
1. Create screen composable in `ui/screens/<feature>/`
2. Create ViewModel if needed (follow existing patterns with `collectAsStateWithLifecycle`)
3. Add navigation route in `MainActivity.kt` or `MainScreen.kt`
4. Use `ThemeProvider` and `GlassBackground` for consistent theming

### Modifying AI Services
- AI services are in `domain/services/ai/`
- Network calls go through `AiNetworkClient`
- Always handle coroutines with proper error handling
- Log with Timber for debugging

### Working with Database
- Entities in `data/database/`
- Use DAOs with Flow for reactive updates
- Database instance created in Application class via Room

### Updating Dependencies
- Version catalog in `gradle/libs.versions.toml` (managed by Gradle)
- Firebase BOM and Compose BOM manage transitive versions
- Uncommented dependencies in `app/build.gradle.kts` are actively used

## Permissions & Security

The app requires:
- `INTERNET`: API calls and Termux bridge
- `com.termux.permission.RUN_COMMAND`: LaTeX compilation
- Storage permissions: Document access
- `FOREGROUND_SERVICE_DATA_SYNC`: Background processing
- `POST_NOTIFICATIONS`: Compilation progress notifications

Network security: `android:usesCleartextTraffic="true"` enabled for Termux localhost communication.

## Code Style Conventions

- Compose over XML layouts (no XML layout files)
- State hoisting for composables
- ViewModels use StateFlow/Flow with `collectAsStateWithLifecycle`
- Timber for logging, never direct `Log.*`
- Coroutines for async work, avoid GlobalScope except in Application class
- Material 3 components preferred
- Glass-morphism styling via custom components in `ui/components/glass/`

## Debugging Tips

- Use `AppLogger.kt` for structured logging
- Check Timber logs for service execution traces
- Termux bridge issues: Verify Termux server is running and accessible
- AI errors: Check `AiNetworkClient` logs and API key configuration
- Database issues: Use Room's debugging features or export DB via Device File Explorer
