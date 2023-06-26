# Discord – Listening Party Bot
![Blahaj](https://i.imgur.com/yH9Jq4p.png)

A Spotify-powered Discord bot for hosting listening parties! 

**Use the following URL to invite the bot to your server:**\
https://discord.com/oauth2/authorize?client_id=1062043789646118982&permissions=18432&scope=bot

(Make sure the permissions for sending messages and embedding links are checked, otherwise the bot won't work.)

## Basic Usage
Once the bot is invited to your server, make sure it has writing permissions. Once that's done, you can manage listening parties on a per-channel basis (threads aren't supported yet). Here is how you do that:

1. Type `/set` and pass a URL to a Spotify album or playlist (you can get the link by right-clicking on the playlist or album and selecting _Share → Copy Album/Playlist Link_): \
![/set](https://i.imgur.com/M0Buzb5.png)

2. The bot will then verify and preprocess the given target. If everything looks good, a message with the link will pop up, so that everyone can get ready: \
![/set](https://i.imgur.com/eNPYJob.png)

3. Type `/start` to start the actual listening party. A custom countdown (in seconds) can be passed to this command. By default, it's 5 seconds. \
![/start](https://i.imgur.com/fmJn7ab.png)

And that's all there is to it! From here on out, the bot will provide detailed information about whichever song is currently playing in a synchronized manner. When possible, it will also display some additional information of the current song from [last.fm's wiki](https://www.last.fm/music/Apocalyptica/_/I+Don%27t+Care/+wiki):

![last.fm wiki](https://i.imgur.com/ebv9jUu.png)

This continues until every song of the given playlist or album has been played. At the end, you'll get a random final message:

![Final Message](https://i.imgur.com/X0lUDkB.png)

## Commands
Here's a full list of every command and what it does:

### `/np` ("now playing")
Print info of the currently ongoing Listening Party for this channel. Useful for late joiners, because it also displays the timestamp of the current song:

![/nowplaying](https://i.imgur.com/LIlXCop.png)

### `/set <url>`
Set the target link (_url_: the URL to the Spotify playlist or album)

### `/start <countdown>`
Start or resume the Listening Party (_countdown_: the seconds to count down, default: 5)

### `/quickstart <url>`
A combination of `/set` and `/start` to instantly start a Listening Party without countdown (_url_: the URL to the Spotify playlist or album)

### `/stop`
Cancel a current Listening Party and reset it to the beginning

### `/skip <amount>`
Skip the current song in the Listening Party (_amount_: how many songs to skip, default: 1)

### `/previous <amount>`
Play the previous song in the Listening Party (_amount_: how many songs to go back to, default: 1)

### `/restart`
Restart the currently playing song

### `/pause`
Pause the current Listening Party (resume by typing `/start` again)

### `/link`
Print the set target link

### `/help`
Print a basic tutorial of how the bot works

### `/commands`
Print all commands as a chat message

### `/custom <attachment>`
_[Experimental]_ Host a party custom-defined by the given attachment. **This feature is highly experimental and therefore shouldn't be used unless you know what you're doing!**

## Support
If you got any problems, [write an issue ticket on GitHub](https://github.com/Selbi182/ListeningPartyBot/issues) and I will gladly take a look at it :)

Alternatively, message me on Discord: **Selbi#7270**
