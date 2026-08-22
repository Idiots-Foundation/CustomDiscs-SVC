[![Modrinth downloads](https://img.shields.io/modrinth/dt/1yowwDpk?logo=modrinth&label=downloads)](https://modrinth.com/plugin/customdiscs-svc)

# CustomDiscs

CustomDiscs brings custom, positional music discs to Minecraft servers through
[Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat). Turn a regular music disc into a custom one backed by
a local MP3, WAV, or FLAC file, or stream a track from a supported remote service such as YouTube or SoundCloud.

Audio is played from the jukebox position, so players hear it naturally as they move through the world. Server owners can
control playback range, volume, track limits, disc appearance, visualizations, and remote provider behavior. Developers can
integrate with playback through the public API and Bukkit events.

## Special thanks

CustomDiscs builds on ideas and technology from these projects:

- [Navoei CustomDiscs](https://github.com/Navoei/CustomDiscs)
- [AudioPlayer by henkelmax](https://github.com/henkelmax/audio-player)
- [LavaPlayer](https://github.com/sedmelluq/lavaplayer)

## Links

- [Modrinth](https://modrinth.com/plugin/customdiscs-svc)
- [GitHub](https://github.com/Idiots-Foundation/CustomDiscs-SVC)
- [API documentation](#developer-api)
- [Maven repository](https://repo.subkek.space/maven-public/)
- [Discord support](https://discord.gg/eRvwvmEXWz)


## Features

- Local playback from MP3, WAV, and FLAC files
- Remote playback from YouTube and SoundCloud URLs
- Positional audio powered by Simple Voice Chat
- Per-jukebox playback radius
- Custom model data for local and remote discs
- Configurable particles and playback holograms
- Configurable maximum file size and track duration
- English and Russian translations
- Folia-aware scheduling
- Public API, lifecycle events, and audio packet handlers

## Requirements

CustomDiscs requires the following server plugins:

- [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat)
- [PacketEvents](https://modrinth.com/plugin/packetevents)

Players also need the Simple Voice Chat client mod to hear custom disc audio.

## Installation

1. Install Simple Voice Chat and PacketEvents on the server.
2. Download CustomDiscs from [Modrinth](https://modrinth.com/plugin/customdiscs-svc/versions).
3. Place all three plugin files in the server's `plugins` directory.
4. Start or restart the server.
5. Review the generated configuration in `plugins/CustomDiscs/config.yml`.
6. Make sure Simple Voice Chat is configured and working before testing a disc.

## Creating a disc

Custom discs are created from an ordinary Minecraft music disc held in the player's main hand. Arguments containing spaces
must be enclosed in double quotes.

### From a local file

Place an MP3, WAV, or FLAC file in `plugins/CustomDiscs/musicdata`, or download one from a direct HTTP(S) URL:

```text
/cd download "https://example.com/music/track.mp3" track.mp3
```

Hold a music disc and convert it:

```text
/cd create local track.mp3 "Track name"
```

### From a remote service

Hold a music disc and provide a supported URL:

```text
/cd create remote "https://www.youtube.com/watch?v=..." "Track name"
```

Insert the resulting disc into a jukebox to start positional playback.

> [!WARNING]
> Remote services commonly require additional configuration before playback will work reliably. YouTube may require OAuth2,
> a PO token, a remote cipher server, or an HTTP(S) proxy. Provider behavior can change independently of CustomDiscs. Use a
> secondary account for OAuth2 authorization and never publish provider credentials or tokens.

Only stream or download audio that you are authorized to use. Server owners are responsible for complying with the terms of
the source service and applicable copyright law.

## Commands

The full command is `/customdiscs`; `/cd` is available as a shorter alias.

| Command                                     | Description                                                             | Permission                  |
|---------------------------------------------|-------------------------------------------------------------------------|-----------------------------|
| `/cd`                                       | Root CustomdDiscs command                                               | `customdiscs`               |
| `/cd help`                                  | Shows the commands available to the sender.                             | `customdiscs.help`          |
| `/cd reload`                                | Reloads configuration and language files.                               | `customdiscs.reload`        |
| `/cd download "<direct URL>" <filename>`    | Downloads an MP3, WAV, or FLAC file into `musicdata`.                   | `customdiscs.download`      |
| `/cd create local <filename> "<disc name>"` | Converts the held music disc using a local file.                        | `customdiscs.create.local`  |
| `/cd create remote "<URL>" "<disc name>"`   | Converts the held music disc using a supported remote URL.              | `customdiscs.create.remote` |
| `/cd distance <radius>`                     | Prompts the player to select a jukebox and assigns its playback radius. | `customdiscs.distance`      |

`create local`, `create remote`, and `distance` are player-only operations.

## Permissions

| Permission                             | Default   | Description                                   |
|----------------------------------------|-----------|-----------------------------------------------|
| `customdiscs.*`                        | Operators | Grants every CustomDiscs permission.          |
| `customdiscs.help`                     | Everyone  | Allows access to command help.                |
| `customdiscs.reload`                   | Operators | Allows configuration and language reloads.    |
| `customdiscs.download`                 | Operators | Allows downloading audio files to the server. |
| `customdiscs.create`                   | Everyone  | Parent permission for disc creation.          |
| `customdiscs.create.local`             | Everyone  | Allows creating discs from local files.       |
| `customdiscs.create.remote`            | Everyone  | Allows creating discs from remote URLs.       |
| `customdiscs.create.remote.youtube`    | Everyone  | Allows creating discs from YouTube URLs.      |
| `customdiscs.create.remote.soundcloud` | Everyone  | Allows creating discs from SoundCloud URLs.   |
| `customdiscs.distance`                 | Everyone  | Allows changing a jukebox's playback radius.  |

## Configuration

The plugin creates and maintains `plugins/CustomDiscs/config.yml`. The file includes comments describing each option, so it
can be configured directly without copying a complete template from this README. Important settings include:

- language, update checks, and debug logging;
- maximum download size and playback radius;
- master volume and maximum track length;
- custom model data for local, YouTube, and SoundCloud discs;
- URL suggestions and provider matching rules;
- particle, hologram, or combined jukebox visualizations;
- hologram render distance, name length, position mode, and offsets;
- YouTube OAuth2, PO token, remote cipher server, and HTTP(S) proxy settings.

Run `/cd reload` after editing the configuration. Language files are stored in `plugins/CustomDiscs/language` and can also be
customized.

## Developer API

The API artifact is published from the project's Maven repository. Replace the version below if a newer API release is
available.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.subkek.space/maven-public/")
}

dependencies {
    compileOnly("space.subkek:customdiscs-api:1.0.0-SNAPSHOT")
}
```

### Maven

```xml
<repository>
    <id>subkek</id>
    <url>https://repo.subkek.space/maven-public/</url>
</repository>

<dependency>
    <groupId>space.subkek</groupId>
    <artifactId>customdiscs-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Declare CustomDiscs and Simple Voice Chat as server dependencies in your Paper plugin descriptor. You can then obtain the
registered API service:

```java
import space.subkek.customdiscs.api.CustomDiscsAPI;

CustomDiscsAPI api = CustomDiscsAPI.get();
if (api ==null)
    throw new IllegalStateException("CustomDiscs is not loaded");
boolean customDisc = api.isCustomDisc(itemStack);
api.getLavaPlayerManager().play(jukeboxBlock, audioIdentifier, null);
```

The API exposes:

- `CustomDiscsAPI` for service discovery, disc identification, and playback manager access;
- `LavaPlayerManager` for starting, stopping, and inspecting positional playback;
- packet handlers for observing or filtering encoded audio frames;
- `CustomDiscInsertEvent` and `CustomDiscEjectEvent` for jukebox interaction;
- `LavaPlayerStartPlayingEvent` and `LavaPlayerStopPlayingEvent` for playback lifecycle hooks.

The LavaPlayer lifecycle events may run asynchronously or on a region thread. Schedule Bukkit world operations on the
appropriate server or region scheduler.

## Building from source

The project requires JDK 21 and includes the Gradle Wrapper:

```bash
./gradlew build
```

The distributable plugin JAR is produced in `build/libs`.
