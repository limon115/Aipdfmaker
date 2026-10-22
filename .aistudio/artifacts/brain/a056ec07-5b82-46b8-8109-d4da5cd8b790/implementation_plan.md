# High-Performance LaTeX Editor, Instant Search Cache & Side-by-Side Split View

Comprehensive architectural plan to eliminate editor and search input lag, implement precomputed background search caching, optimize syntax highlighting and recomposition bottlenecks, and add a flexible side-by-side Split View for simultaneous LaTeX code editing and live PDF preview.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions are established based on user feedback and performance profiling:

- **Confirmed Decision 1 (Split View):** Implement a dedicated side-by-side Split View layout enabling simultaneous LaTeX code editing and live compiled PDF preview, with smooth orientation/screen adaptation (side-by-side on tablets/landscape, adjustable split or instant tab switch on compact portrait).
- **Confirmed Decision 2 (Full-Code Search Cache):** Cache the full LaTeX document upon viewing in an indexed search structure (precomputed line maps, normalized character offsets, and token boundaries) so queries execute in under 2ms without re-scanning or stalling the main UI thread.
- **Confirmed Decision 3 (Decoupled Typing Architecture):** Decouple local editor `TextFieldValue` state mutations from the global `NotesViewerViewModel` StateFlow. ViewModel updates will be debounced (300ms) to eliminate full-screen recompositions of the 990-line screen during typing.
- **Confirmed Decision 4 (Optimized Syntax Highlighting Engine):** Replace the multi-pass, six-regex whole-document scanning `LatexSyntaxVisualTransformation` with an incremental or viewport-scoped tokenizer with compiled DFA patterns and match caching, preventing frame drops during rapid typing and cursor movement.

---

## 1. Overview & Core Concept

### What It Does
Provides an ultra-responsive, professional mobile LaTeX editing environment within the document viewer. Users can type, scroll, and search through thousands of lines of LaTeX source code without stuttering or latency, while simultaneously viewing the compiled PDF preview in a responsive split screen.

### Target Audience & Persona
Researchers, educators, students, and engineers writing mathematical notes, textbook chapters, and technical articles on Android devices who need rapid code manipulation and immediate visual feedback.

### Key Value
- **Zero-lag typing:** 60fps/120fps fluid keystroke entry even in large documents.
- **Instant search:** Sub-millisecond search match calculations using cached token indices.
- **Simultaneous editing & inspection:** Edit formulas in LaTeX while observing the rendered layout in the adjacent PDF pane.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Viewing & Editing:**
   - User opens a LaTeX document in `NotesViewerScreen`.
   - The editor immediately displays three view modes in the top toolbar: **Source**, **Split View**, and **Preview**.
   - In **Split View**, the screen displays the LaTeX code editor on the left (or top in portrait) and the compiled PDF preview on the right (or bottom in portrait), with an intuitive divider handle.

2. **Ultra-Fast Search:**
   - User taps the Search icon (or presses Ctrl+F).
   - An integrated search bar slides in with match counters (`1 of 14`), previous/next navigation buttons, and match jumping.
   - Matches are resolved instantly against the precomputed document cache.
   - Navigating matches automatically centers the editor viewport on the target line with a highlighted selection.

3. **Live Sync & Recompile:**
   - Quick toolbar action for **Recompile** with animated progress indicator.
   - Floating accessory bar provides instant access to common LaTeX symbols (`\`, `{ }`, `[ ]`, `$`, `^`, `_`, `&`, `%`, `\begin`, `\frac`, `\int`) and Undo/Redo.

### Visual Identity & Theme
- **Color Tokens:**
  - Editor Background: `#FFFFFF` (Surface) / `#0F172A` (Dark mode)
  - Active Line Highlight: `#EFF6FF` (Subtle blue tint)
  - Search Match Highlight: `#FEF08A` (Soft yellow), Active Match: `#FDBA74` (Vibrant amber)
  - Syntax Highlighting: Commands `#0284C7` (Sky blue), Math `#059669` (Emerald), Numbers `#C026D3` (Fuchsia), Comments `#64748B` (Slate)
- **Typography:**
  - JetBrains Mono or System Monospace for LaTeX source code with proportional line heights.
  - Inter / Roboto for UI labels and match counters.
- **Icons:** Crisp Material Symbols SVG icons exclusively (no emojis).

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Search Architecture — Precomputed In-Memory Index vs Real-Time Grep
- **Chosen Approach:** Background precomputed full-document cache upon document load. Creates a normalized token index and line-offset lookup table.
- **Why:** Eliminates all UI thread stalling. When typing a query, results are fetched against precomputed character arrays or cached regex matches without re-scanning thousands of characters on every keystroke.
- **Alternatives Considered:** Real-time `indexOf` on every frame (caused current lag).

### Decision 2: State Flow Isolation
- **Chosen Approach:** Keep high-frequency `TextFieldValue` state strictly local to the editor composable and isolate the recomposition boundary. Propagate changes upstream to `NotesViewerViewModel` via a debounced handler (300ms) and on blur/recompile.
- **Why:** Prevents the outer `NotesViewerScreen` (containing top bars, dialogs, bottom sheets, and PDF renderers) from recomposing on every character typed.
- **Alternatives Considered:** Updating `NotesViewerViewModel._state` on every single keystroke (caused whole-screen invalidation).

### Decision 3: Syntax Highlighting Optimization
- **Chosen Approach:** Tokenizer that caches highlighted `AnnotatedString` spans and only re-evaluates changed text or uses lightweight single-pass scanning instead of six separate regex passes over the entire string.
- **Why:** In Android Jetpack Compose, `VisualTransformation.filter()` is invoked on layout changes, cursor position changes, and text edits. Multi-pass regexes over entire documents cause noticeable input freeze.

### Decision 4: Split View Layout Strategy
- **Chosen Approach:** Dynamic layout using `BoxWithConstraints` that switches between side-by-side horizontal split (when width >= 600dp or landscape) and vertical split/toggle (on compact portrait), with an easy pane toggle button.
- **Why:** Maximizes productivity on tablets, foldables, and desktop/emulator windows while remaining clean and usable on phones.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          NotesViewerScreen                             │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Top Bar: Mode Selector [ Source | Split | Preview ] | Recompile  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                    │                                   │
│            ┌───────────────────────┴───────────────────────┐            │
│            ▼                                               ▼            │
│   ┌───────────────────────────────┐               ┌─────────────────┐  │
│   │   LatexCodeEditorContainer    │               │  PdfDocument-   │  │
│   │  (Isolated Recomposition)     │               │     Preview     │  │
│   │ ┌───────────────────────────┐ │               │   (Rendered     │  │
│   │ │ Cached Search Controller  │ │               │     PDF)        │  │
│   │ │ - Full-text token index   │ │               │                 │  │
│   │ │ - Fast binary match jump  │ │               │                 │  │
│   │ └───────────────────────────┘ │               │                 │  │
│   │ ┌───────────────────────────┐ │               │                 │  │
│   │ │ Single-Pass Syntax Engine │ │               │                 │  │
│   │ │ - Span cache              │ │               │                 │  │
│   │ │ - Zero cursor drift       │ │               │                 │  │
│   │ └───────────────────────────┘ │               │                 │  │
│   │ ┌───────────────────────────┐ │               │                 │  │
│   │ │ Gutter & Code Editor      │ │               │                 │  │
│   │ │ - Optimized Canvas gutter │ │               │                 │  │
│   │ │ - Fast BasicTextField     │ │               │                 │  │
│   │ └───────────────────────────┘ │               │                 │  │
│   └───────────────────────────────┘               └─────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Structures & State Management
- `LatexSearchIndex`: In-memory index created upon document load:
  - `rawText`: Cached string reference
  - `lineOffsets`: `IntArray` mapping line numbers to character offsets for instant line navigation.
  - `search(query: String)`: Non-blocking search returning list of `TextRange` matches.
- `EditorPerformanceConfig`:
  - Debounce interval: 300ms for ViewModel sync
  - Syntax highlighting cache: Remembers span calculations by text hash.

---

## 5. Verification & Testing Plan
1. **Compilation Check:** Run `compile_applet` to ensure zero compilation or syntax errors.
2. **Keystroke Performance Verification:** Verify continuous rapid typing produces no input lag or dropped frames.
3. **Search Responsiveness:** Verify typing in the search bar produces immediate match counts and smooth navigation between matches.
4. **Split View Validation:** Verify toggling between Source, Split, and Preview modes renders code and PDF smoothly side by side.
