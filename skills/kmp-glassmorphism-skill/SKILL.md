---
name: kmp-glassmorphism-ui
description: >
  Implements a production-grade Glassmorphism design system for Kotlin Multiplatform (KMP)
  Compose Multiplatform projects. Use when the user asks for glass UI, frosted glass effects,
  glassmorphism theme, translucent/transparent UI components, liquid/blob animated backgrounds,
  or premium modern dark-mode design in a Compose Desktop, Android, or multiplatform app.
version: 1.0.0
---

# KMP Glassmorphism UI Design System Skill

## Overview

Glassmorphism is a modern UI design approach characterized by:
- **Semi-transparent backgrounds** with gradient opacity
- **Frosted-glass borders** using gradient strokes
- **Background blur** (where supported by the rendering engine)
- **Layered depth** with multiple translucent surfaces over animated backgrounds
- **Vibrant accent colors** that bleed through transparent layers

This skill provides a complete, production-tested design system for **Compose Multiplatform** (Desktop JVM + Android + iOS + WASM). All components use `androidx.compose.material3.MaterialTheme` for theming and work with both dark and light modes.

**Technology requirements:**
- Kotlin 1.9+
- Compose Multiplatform 1.5.0+
- Material3 (`compose.material3`)
- Material Icons Extended (`compose.materialIconsExtended`)

---

## Step 1: Add the Theme System

Copy `assets/theme/GlassTheme.kt` into your project's theme package.

The theme provides:
- **Dark colors**: Deep navy background (`#0A0C14`), dark surface (`#161922`), white text
- **Light colors**: Soft gray-blue background (`#F0F2F5`), white surface, dark text
- **Accent colors**: Purple primary (`#6C63FF`), Cyan secondary (`#00E5FF`), Red error (`#FF5252`), Camo green success (`#507652`)
- **Typography**: SansSerif family with Bold headlines, Medium subheads, Normal body, SemiBold labels

Wrap your app root composable with `GlassTheme`:

`kotlin
GlassTheme(darkTheme = true) {
    // Your app content
}
`

### Customizing Colors

Modify the color constants at the top of `GlassTheme.kt`:

`kotlin
val GlassAccent = Color(0xFF6C63FF)         // Primary brand color
val GlassAccentSecondary = Color(0xFF00E5FF) // Secondary/accent
`

All glass components read colors from `MaterialTheme.colorScheme`, so changing the theme automatically updates every component.

---

## Step 2: Add the Background Layer

Copy `assets/background/LiquidBackground.kt` into your components package.

`LiquidBackground` creates an immersive backdrop with animated radial gradient blobs. Place it as the **outermost container** beneath all glass surfaces.

`kotlin
LiquidBackground {
    GlassSurface { /* content */ }
}
`

### Configuration

| Parameter | Type | Default | Description |
|---|---|---|---|
| `enableTransparentBackground` | `Boolean` | `false` | Uses `Color.Transparent` for window-level transparency |
| `enableAnimation` | `Boolean` | `false` | Blobs drift using `InfiniteTransition`. Increases GPU usage |

Default creates 4 blobs: Purple 400dp top-left, Cyan 350dp center-right, Red 300dp bottom-left, Purple 500dp bottom-right.

---

## Step 3: Primitive Components

### GlassSurface
Copy `assets/primitives/GlassSurface.kt`. Foundation component. Linear gradient background at configurable alpha + gradient border stroke.

`kotlin
GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), alpha = 0.5f) {
    Text("Content on glass")
}
`

### GlassCard
Copy `assets/primitives/GlassCard.kt`. Premium card with radial white glow + 3-stop shimmer border.

`kotlin
GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
    Column(Modifier.padding(24.dp)) { Text("Premium card") }
}
`

---

## Step 4: Input Components

### GlassTextField
Copy `assets/inputs/GlassTextField.kt`. Text input built on GlassSurface with BasicTextField, leading/trailing icon slots.

`kotlin
GlassTextField(value = text, onValueChange = { text = it }, placeholder = "Type here...",
    leadingIcon = Icons.Default.Search, singleLine = true)
`

### GlassButton
Copy `assets/inputs/GlassButton.kt`. Pill-shaped button on GlassCard with hover gradient (30% -> 80% alpha).

`kotlin
GlassButton(onClick = { }, enabled = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Send, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Send")
    }
}
`

---

## Step 5: Navigation Components

### GlassDropdown
Copy `assets/navigation/GlassDropdown.kt`. String-based dropdown selector.

### GlassRichDropdown<T>
Copy `assets/navigation/GlassRichDropdown.kt`. Generic type-safe dropdown with custom trigger/item slots.

### GlassSidebar
Copy `assets/navigation/GlassSidebar.kt`. Translucent sidebar panel with slot-based content.

`kotlin
GlassSidebar(modifier = Modifier.width(260.dp), alpha = 0.2f) { /* content */ }
`

---

## Step 6: Feedback Components

### GlassChatBubble
Copy `assets/feedback/GlassChatBubble.kt`. Chat bubble with asymmetric corners and SelectionContainer.

`kotlin
GlassChatBubble(isUser = true,
    userIcon = { Icon(Icons.Default.Person, "User", tint = onSurface) },
    aiIcon = { Icon(Icons.Default.SmartToy, "AI", tint = onSurface) }
) { Text("Hello!") }
`

---

## Step 7: Window Transparency (Desktop)

See `references/transparency-guide.md`. Quick setup:

`kotlin
fun main() = application {
    Window(onCloseRequest = ::exitApplication, transparent = true, undecorated = true) {
        GlassTheme(darkTheme = true) {
            Surface(modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                shape = RoundedCornerShape(16.dp)) {
                LiquidBackground(enableTransparentBackground = true) { /* content */ }
            }
        }
    }
}
`

---

## Decision Tree

1. **Themed background** -> `LiquidBackground` + `GlassTheme`
2. **Container/panel** -> Generic: `GlassSurface` | Premium: `GlassCard` | Sidebar: `GlassSidebar`
3. **Input** -> Text: `GlassTextField` | Action: `GlassButton`
4. **Selection** -> Simple: `GlassDropdown` | Complex: `GlassRichDropdown<T>`
5. **Chat UI** -> `GlassChatBubble` + `GlassTextField` + `GlassButton`
6. **Transparent window** -> `references/transparency-guide.md`
7. **Animated background** -> `LiquidBackground(enableAnimation = true)`

## Alpha Guidelines

| Use Case | Alpha |
|---|---|
| Main content surfaces | `0.5f` |
| Sidebar panels | `0.2f` |
| Modal dialogs | `0.95f` |
| Dropdown menus | `0.9f - 0.95f` |
| Hover state backgrounds | `0.8f` |
