# ZenBounce 🎮

**Tilt · Bounce · Relax**

A meditative physics-based Android game where you tilt your device to control bouncing balls. Choose from 4 different ball types, each with unique physics and visual styles. Customize your experience with 6 stunning visual themes.

## Features

✨ **4 Unique Bounce Objects**
- **ZenBall** (Default) - Balanced physics, theme-aware colors
- **Ping Pong Ball** - Light, fast, high bounce
- **Tennis Ball** - Medium weight, yellow-green, authentic seams
- **Football** - Heavy, low bounce, pentagonal panel design

🎨 **6 Visual Themes**
- Dark Neon (default)
- Amoled Night
- Ocean Deep
- Aurora
- Sunset
- Zen White

⚙️ **Core Mechanics**
- **Device Tilt Control** - Use your device's gyroscope to guide the ball
- **Physics Engine** - Realistic collision with wall elasticity and damping
- **Sensitivity Control** - Adjust how responsive the ball is to tilting (0-100)
- **Trail Animation** - Visual trail follows the ball's path
- **Particle System** - Ambient background particles for visual depth
- **Haptic Feedback** - Collision vibrations (requires Android 13+)
- **Sound Effects** - Per-object collision sounds (files needed - see TODO.md)
- **Pause/Resume** - Full game pause with lifecycle-aware restoration

## Architecture

This project demonstrates clean Android architecture with Compose:

### Modules
- **game/** - Physics engine, game state, ViewModel
- **objects/** - BounceObject catalog, physics/visual definitions
- **theme/** - Theme presets, theme persistence
- **sensors/** - Gyroscope/accelerometer input  
- **prefs/** - User preferences (sensitivity, theme, object selection)
- **haptics/** - Vibration feedback on collisions
- **sound/** - Collision audio effects
- **ui/screens/** - Game, Menu, Settings, Ball Picker screens
- **ui/components/** - Canvas rendering, particle effects

### Key Patterns
- **MVVM** with Jetpack Lifecycle
- **Coroutines** for async operations
- **Flow** for reactive state management
- **DataStore** for persistent preferences
- **Compose Canvas** for custom rendering
- **Pure Physics Engine** (stateless, testable)

## Building

```bash
# Clone the repo
git clone https://github.com/dineshvarma97/ZenBounce.git

# Open in Android Studio (AGP 8.7.0, Kotlin 2.0.0)
# Minimum SDK: 33
# Target SDK: 34

# Build
./gradlew build

# Run on device
./gradlew installDebug
```

## Requirements

- Android 13+ (API 33+)
- Device with gyroscope/accelerometer sensor
- Optional: Haptic engine support (Android 13+) for vibrations

## Next Steps

See [TODO.md](TODO.md) for remaining tasks:
- Sound file integration (4 OGG files needed)
- Optional test coverage expansion
- Future object additions

## License

MIT License - see LICENSE file

## Author

Dinesh Varma
