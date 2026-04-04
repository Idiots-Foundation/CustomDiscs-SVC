## 2.5.2

### Fixed
- `data.yml` was being rewritten on every autosave tick even when nothing changed — introduced a dirty flag so the file is only saved when data has actually been modified
- `data.yml` was not saved on plugin shutdown
- `allow-hoppers` config option is now `true` by default
- Setting a per-jukebox distance via `/cd distance` did not mark data as dirty, so the value was lost on restart

### Language
- Removed unused key `error.play.no-permission` from both locales
- Removed unused key `error.command.no-youtube-support` from `en_US` (was also missing from `ru_RU`)
