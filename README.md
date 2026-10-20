# OneWayElytra

Do you want to glide from your spawn into the unknown? Even without ever entering the end? This plugin adds a one-way-elytra for your spawn. Players are able to fly into the air and boost once they are in the air.

This branch is the **Folia port** of OneWayElytra, targeting **Minecraft/Folia 26.2** (Java 25).

<img src="/.github/pictures/example.gif" width="400">

## Features
- 🪽 fly into the air without an elytra
- 🚀 boost once while flying
- ▦ optional WorldGuard flags
- 🧵 Folia-ready: all player state is handled on the player's own region thread

## Requirements
| | Version |
|---|---|
| Server | Folia 26.2 |
| Java | 25+ |
| WorldGuard | optional, 7.0.20+ |

> This build uses Folia's region and entity schedulers directly, so it is **not** compatible with regular Paper/Spigot servers.
>
> WorldGuard 7.x is not officially Folia-compatible. The hook is optional and only used when WorldGuard is installed and loadable, so the plugin works without it.

## Installation
1. Download the `OneWayElytra-<version>-folia-26.2.jar` from the latest release.
2. Put it into your server's `plugins` folder.
3. Start the server. The default `config.yml` and `languages/EN.yml` are created automatically.

## Commands
**/onewayelytra | /owe**
_Shows you all the commands._

**/onewayelytra location <add/delete> [name]**
_Add or delete a location._

**/onewayelytra location set [name] radius [number]**
_Set the location's radius._

**/onewayelytra location list**
_List all locations._

**/onewayelytra tag <add/remove/info>**
_Add, remove, or inspect the OneWayElytra tag on the equipped chestplate._

### Permissions
| Permission | Default | Description |
|---|---|---|
| `onewayelytra.command` | op | Allows using `/onewayelytra`. |

## WorldGuard
The following flags are registered:

| Flag | Default | Description |
|---|---|---|
| `owe-start` | `deny` | Allows starting a one-way elytra flight inside the region. |
| `owe-entry` | `allow` | Allows a flying player to enter the region. |
| `owe-item` | `allow` | Allows using a tagged elytra item inside the region. |

Examples:
```
/rg flag <region> owe-start allow
/rg flag <region> owe-entry deny
/rg flag <region> owe-item deny
```

## Configuration

<details>
<summary>config.yml</summary>

```yaml
##
## OneWayElytra | config.yml
##

# Do not change anything unless you know what you are doing!
config-version: 1

# Available languages (stored inside the jar-file): EN, DE
language: EN

# default radius in blocks
radius: 16

# multiplier for the boost (standard: 5)
boostMultiplier: 5

# allow gliding in adventure mode
adventure: true

locations: {}
```
</details>

## Building
Folia 26.2 is compiled for Java 25.

```bash
mvn clean package
```

The shaded plugin is written to `target/OneWayElytra-<version>.jar`.

The GitHub Actions workflow in `.github/workflows/main.yml` builds the plugin on every push to `master`/`main` and publishes the Folia 26.2 JAR as a GitHub release.

## License
See [LICENSE](LICENSE).
