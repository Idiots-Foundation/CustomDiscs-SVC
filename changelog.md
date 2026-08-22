# CustomDiscs-SVC 2.7.0

> [!IMPORTANT]
> This update is being released solely to restore remote source playback. It is an interim release ahead of a larger update that will improve the plugin, simplify its behavior, and make it easier to understand and use.

## Fixed

- Restored remote source playback by updating the YouTube source implementation and expanding the available YouTube clients.
- Improved jukebox playback and visualization cleanup when discs are stopped or ejected.

## Added

- Added configurable jukebox visualizations with four modes: `particles`, `hologram`, `both`, and `off`.
- Added configurable hologram render distance, track name length, position mode, and position offsets.
- Added localized hologram text for English and Russian.
- Added custom disc insert handling for hopper and redstone-driven jukebox interactions.
- Added packet-based jukebox handling to keep custom playback behavior synchronized with the client.

## Changed

- Refactored the command system around Paper's native Brigadier command API.
- Improved command permission checks, subcommand registration, and quoted argument suggestions.
- Refactored Bukkit event handlers into dedicated listener classes.
- Reworked jukebox visualization rendering and activation-side positioning.
- Updated the README with current installation, command, permission, configuration, API, and compatibility information.
- Shaded and relocated Simple YAML into the plugin jar to simplify runtime dependency loading.
- Updated Paper API, Simple Voice Chat API, Lavaplayer, PacketEvents, FoliaLib, Commons IO, Lombok, Shadow, Paper YML, and the Gradle wrapper.
- Updated supported Minecraft versions to 1.21.6 and newer, including 26.2.

## Internal

- Applied project-wide Java style cleanup and general code simplification.
- Simplified the command class hierarchy and removed obsolete event-handler code.
