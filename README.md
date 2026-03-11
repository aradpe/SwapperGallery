# SwapperGallery

A non-destructive photo editor for Android that saves edits in-place without creating duplicate files, while keeping all edits fully modifiable.

## The Problem

Built-in gallery apps (Samsung Gallery, Google Photos, etc.) bake edits into the image when saving. This means:
- Edits create a new file (duplicate) or destroy the original
- You can't go back and modify individual edits (move text, change a filter, remove a drawing)
- The edited version treats all changes as permanent pixels

## The Solution

SwapperGallery overwrites the original file (no duplicates), but stores edit metadata separately so every edit remains individually editable:

1. **Open** an image and add a text overlay
2. **Save** - the file is overwritten with the edited version
3. **Reopen** - the text overlay is still a separate, editable layer
4. **Move** the text, change its color, or delete it entirely
5. **Save** again - file is overwritten, but all remaining edits stay editable

## Features

### Gallery
- Browse all device photos in a grid view
- Organize by albums
- Shows "edited" badge on photos with active edit projects
- Open images from other apps via share/intent

### Non-Destructive Editor
- **Text** - Add text overlays with font, color, size, bold/italic, outline, and background options. Tap to reposition.
- **Drawing** - Freehand brush with adjustable color, size, opacity. Eraser tool included.
- **Crop & Rotate** - Free crop, aspect ratio presets (1:1, 4:3, 3:2, 16:9), rotation.
- **Filters** - 12 preset filters (B&W, Sepia, Vintage, Vivid, Cool, Warm, Noir, Fade, Dramatic, Chrome, Invert, Pastel) with adjustable intensity.
- **Adjustments** - Brightness, contrast, saturation, warmth, sharpness, highlights, shadows, vignette sliders.
- **Stickers** - 50+ emoji stickers with position, scale, and rotation.
- **Blur** - Full image blur, radial blur (bokeh), with adjustable intensity and focal point.

### Layer System
- Every edit is a layer that can be toggled on/off, deleted, or modified
- Layer panel shows all edits with visibility toggle and delete button
- Undo/redo support

## Architecture

```
User's Gallery
  photo.jpg <- always shows latest edited version
       |
  SwapperGallery Engine
       |
  Original Backup + Layer Metadata (Room DB)
       |
  Composite -> overwrite file
```

- **Original backup** stored in app-private storage
- **Edit layers** stored as JSON in Room database
- **On save**: original + layers composited -> overwrites the file
- **On reopen**: loads backup + layers -> fully editable

## Tech Stack

| Technology | Purpose |
|---|---|
| Kotlin | Language |
| Jetpack Compose | UI framework |
| Material 3 | Design system |
| Room | Local database for edit metadata |
| Hilt | Dependency injection |
| Coil | Image loading |
| Navigation Compose | Screen navigation |
| Kotlinx Serialization | Layer data serialization |
| Android Canvas/Paint | Image compositing and rendering |

## Requirements

- Android 8.0+ (API 26)
- Android Studio Hedgehog (2023.1.1) or later

## Building

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/swappergallery/
├── data/
│   ├── db/          # Room database, DAOs, type converters
│   ├── model/       # Entities (EditProject, EditLayer, LayerData, MediaItem)
│   └── repository/  # GalleryRepository, EditRepository, BackupManager
├── di/              # Hilt modules
├── ui/
│   ├── gallery/     # Photo grid + albums
│   ├── viewer/      # Full-screen image viewer
│   ├── editor/      # Non-destructive editor
│   │   ├── tools/   # Text, Draw, Crop, Filter, Adjust, Sticker, Blur
│   │   └── components/ # LayerPanel, ColorPicker, ToolBar, SliderControl
│   ├── navigation/  # NavGraph and routes
│   └── theme/       # Material 3 theme
└── util/            # ImageCompositor, FileUtils, PermissionUtils
```
