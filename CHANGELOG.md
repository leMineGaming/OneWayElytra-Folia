## 1.3.0 – Folia 26.2 port

### Breaking changes
- Requires **Folia 26.2** (or Paper 26.2) and **Java 25**.
- The plugin now declares `folia-supported: true` and `api-version: '26.2'`.

### Changes
- Replaced all `BukkitScheduler` usage with Folia's entity and region schedulers. Flight checks run on each player's own entity scheduler, so player state is never touched from a foreign region thread.
- Flight, falling and boost tracking is now thread-safe and keyed by player UUID instead of a non-thread-safe `ArrayList`.
- Radius areas and frequently read config values are cached and published atomically, so config changes made by a command are visible to region threads without unsafe access to the YAML document.
- Config writes are serialised; `/owe location` commands from different regions can no longer corrupt `config.yml`.
- Action bar messages use the Adventure API (`Player#sendActionBar`) instead of the Spigot-specific `Player.Spigot#sendMessage`.
- WorldGuard is now truly optional: the WorldGuard hook sits behind a small `RegionHook` interface, so missing or incompatible WorldGuard builds cannot prevent the plugin from loading.
- WorldGuard flag registration was simplified and made safe against existing flags.
- Added the `onewayelytra.command` permission to `plugin.yml`.
- Added automatic language fallback to `EN` if the configured language file is missing.
- Fixed landing: the player is removed from the flying state before gliding is disabled, so the glide toggle is no longer cancelled by the plugin's own listener.
- Fixed `/owe tag remove` to report when the equipped chestplate is not tagged.
- Config changes made through commands reload the cached boost multiplier and adventure setting immediately.

### Build
- Maven now compiles against `dev.folia:folia-api:26.2.build.7-beta`.
- GitHub Actions builds on JDK 25 and publishes `OneWayElytra-<version>-folia-26.2.jar`.
