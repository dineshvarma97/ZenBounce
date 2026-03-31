# ZenBounce — Action Items

## 🔊 Sound Files (Required to enable collision audio)

- [ ] Download **4 OGG sound files** from [Freesound.org](https://freesound.org) (CC0 license preferred)
  - [ ] `bounce_default.ogg` — neutral synthetic bounce (e.g. a subtle pop or thud)
  - [ ] `bounce_pingpong.ogg` — sharp, high-pitched click (search: "ping pong hit")
  - [ ] `bounce_tennis.ogg` — rubbery mid-pitch pop (search: "tennis ball hit")
  - [ ] `bounce_football.ogg` — deep hollow thud (search: "soccer ball kick" or "football bounce")
- [ ] Place files in `app/src/main/res/raw/`
- [ ] Uncomment `soundResId` lines in `BounceObjectCatalog.kt`:
  ```kotlin
  // In each BounceObject entry in BounceObjectCatalog.kt:
  soundResId = R.raw.bounce_default     // for DEFAULT
  soundResId = R.raw.bounce_pingpong    // for PING_PONG
  soundResId = R.raw.bounce_tennis      // for TENNIS_BALL
  soundResId = R.raw.bounce_football    // for FOOTBALL
  ```
- [ ] Add a `CREDITS.md` file crediting Freesound.org authors (required for CC-BY files)

---

## 🎨 BallPickerScreen ✅ Done

- [x] Created `BallPickerScreen.kt` — full-screen 2-column grid with canvas previews, mass pills, selected ring border and dot indicator

---

## 🧪 Optional: Expand Test Coverage

- [ ] Add `BounceObjectManagerTest` — DataStore persistence round-trip (save ID, retrieve ID)
- [ ] Add `SoundManagerTest` — mock SoundPool, verify `play()` fires on collision

---

## 🌍 Future Expansion Ideas

- [ ] Add more objects to `BounceObjectCatalog.ALL` (basketball, bowling ball, etc.)
- [ ] Add celestial bodies (Moon, Sun) — set `selfGravity` field on `ObjectPhysics` when needed
- [ ] Dynamic radius capping for small screens: `min(boundsW, boundsH) * 0.12f`
- [ ] Add an in-game object switcher FAB (alongside the existing theme picker FAB)
