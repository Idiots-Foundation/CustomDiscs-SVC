# Custom discs for SVC Addon

## Special thanks
[Navoei CustomDiscs](https://github.com/Navoei/CustomDiscs) | [henkelmax AudioPlayer](https://github.com/henkelmax/audio-player) | [sedmelluq lavaplayer](https://github.com/sedmelluq/lavaplayer)

Create music discs using mp3, wav and flac files, and play audio from YouTube. Enhance and create a unique atmosphere in your game world.
## Configuration
```yaml
# The distance from which music discs can be heard in blocks.
music-disc-distance: 16
# The master volume of music discs from 0-1. (You can set values like 0.5 for 50% volume).
music-disc-volume: 1
# The maximum download size in megabytes.
max-download-size: 50
# enable disc cleaning before re-record disc
cleaning-disc: false
# Language of plugin
# Supported en_US, ru_RU, it_IT
locale: 'en_US'
```
## Commands
```
/cd - Help for CustomDiscs
/cd download <direct link> <name.extension> - Downloads music file from URL.
/cd create <file name> "<disc name>" - Creates music disc.
/cd createyt "<video url>" "<disc name>" - Create disc with music from YouTube.
/cd createsc "<sound url>" "<disc name>" - Create disc with music from SoundCloud.
/cd reload - Reloads configuration file.
```
