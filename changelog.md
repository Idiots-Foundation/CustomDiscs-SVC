## 2.5.2

### Added
- HTTP/HTTPS proxy support for LavaPlayer (only YouTube yet) via `providers.youtube.http-proxy` in `config.yml`
  - Format: `[scheme://][user:pass@]host:port`
  - Supports basic auth and both `http://` and `https://` proxy schemes

### Fixed
- Server no longer lags on startup when `LavaPlayerManagerImpl` initializes — all registrations (YouTube, SoundCloud, proxy setup) now happen asynchronously; disc playback waits for initialization to complete before starting
- `data.yml` was being rewritten on every autosave tick even when nothing changed — introduced a dirty flag so the file is only saved when data has actually been modified
- `data.yml` was not saved on plugin shutdown
- `allow-hoppers` config option is now `true` by default

### Security
- `DownloadSubCommand`: reject URLs with non-http/https schemes (e.g. `file://`, `jar://`) to prevent local file read via download command
- `DownloadSubCommand`: replaced `contains("../")` filename check with `Path.normalize()` boundary validation to properly block all path traversal variants
- `HTTPRequestUtils`: reject non-http/https schemes in all outgoing HTTP requests
- `LegacyUtil`: validate `LOCAL_DISC` PDC value on read to prevent path traversal via NBT-edited disc items

### Language
- Added `error.command.invalid-url` key to `en_US` and `ru_RU`
- Removed unused key `error.play.no-permission` from both locales
- Removed unused key `error.command.no-youtube-support` from `en_US`

### Dependencies
- `commandapi` 11.1.0 → 11.2.0
- `packetevents` 2.11.2 → 2.12.0
- `shadow` (Gradle plugin) 9.4.0 → 9.4.1
