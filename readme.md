# Custom discs for SVC Addon

## Special thanks
[Navoei CustomDiscs](https://github.com/Navoei/CustomDiscs) | [henkelmax AudioPlayer](https://github.com/henkelmax/audio-player) | [sedmelluq lavaplayer](https://github.com/sedmelluq/lavaplayer)

Create music discs using mp3, wav and flac files, and play audio from YouTube. Enhance and create a unique atmosphere in your game world.
## Configuration
```yaml
# CustomDiscs Configuration
# Join our Discord for support: https://discord.gg/eRvwvmEXWz
info:
  # Don't change this value
  version: '1.4'
global:
  # Language of the plugin
  # Supported: ru_RU, en_US
  # Unknown languages will be replaced with en_US
  locale: en_US
  debug: false
command:
  download:
    # The maximum download size in megabytes.
    max-size: 50
  create:
    local:
      custom-model: 0
    remote:
      # tabcomplete — Displaying hints when entering remote command
      # filter — Filter for applying custom-model-data to remote disk
      tabcomplete:
        - https://www.youtube.com/watch?v=
        - https://soundcloud.com/
      youtube:
        custom-model: 0
        filter:
          - https://www.youtube.com/watch?v=
          - https://youtu.be/
      soundcloud:
        custom-model: 0
        filter:
          - https://soundcloud.com/
  distance:
    max: 64
disc:
  # The distance from which music discs can be heard in blocks.
  distance: 64
  # The master volume of music discs from 0-1.
  # You can set values like 0.5 for 50% volume.
  volume: '1.0'
  allow-hoppers: true
providers:
  youtube:
    # This may help if the plugin is not working properly.
    # When you first play the disc after the server starts, you will see an authorization request in the console. Use a secondary account for security purposes.
    use-oauth2: false
    # If you have oauth2 enabled, leave these fields blank.
    # This may help if the plugin is not working properly.
    # https://github.com/lavalink-devs/youtube-source?tab=readme-ov-file#using-a-potoken
    po-token:
      token: ''
      visitor-data: ''
    # A method for obtaining streaming via a remote server that emulates a web client.
    # Make sure Oauth2 was enabled!
    # https://github.com/lavalink-devs/youtube-source?tab=readme-ov-file#using-a-remote-cipher-server
    remote-server:
      url: ''
      password: ''
```
## Commands
```
/cd help: Shows command help
/cd reload: Reloads configuration files.
/cd download "<direct link>" <name.extension>: Downloads music file from URL.
/cd create <local|remote>: Creates music disc.
/cd distance <radius>: Sets the radius for the jukebox.
Some agruments must be written in double quotes "string"
```
