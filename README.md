# Listening Party Bot
![Blahaj](https://i.imgur.com/NuEmaM9.png)

A Discord bot used to set up, organize, and host listening parties for Spotify playlists or albums! 

Use the following URL to invite the bot to your server:<br />
https://discord.com/oauth2/authorize?client_id=1062043789646118982&permissions=8590027776&scope=bot

## Basic Usage
Once the bot is invited to your server, you can manage listening parties on a per-channel basis. Here is how you do that:

1. Type `/set` and pass a URL to a Spotify album or playlist (you can get the link by right-clicking on the playlist or album and selecting _Share → Copy Album/Playlist Link_):
![/set](https://i.imgur.com/fHAfr7J.png)


2. The bot will then verify and preprocess the given playlist or album. If everything looks good, a message will pop up, saying that everything is ready, along with a link to the target, so that everyone can get ready:
![/set](https://i.imgur.com/mCwxRZ2.png)


3. Type `/start` to start the actual listening party. A custom countdown (in seconds) can be passed to this command. By default, it's 5 seconds. \
![/start](https://i.imgur.com/qic5gG2.png)

And that's it! From here on out, the bot will provide detailed information about whichever song is currently playing in a synchronized manner. When possible, it will also display some additional information of the current song from [last.fm's wiki](https://www.last.fm/music/Rammstein/_/Ohne+dich/+wiki):

![last.fm wiki](https://user-images.githubusercontent.com/8850085/227275284-484c228c-859f-455b-9349-e1a1a4fb98ac.png)

## Commands
Here's a full list of every command and what it does:

### `/nowplaying` or `/np`
Print info of the currently ongoing Listening Party for this channel. Useful for late joiners, because it also displays the timestamp:

![/nowplaying](https://i.imgur.com/MCN5pZf.png)

### `/set <url>`
Set the target link (url: the URL to the Spotify playlist or album)

### `/start <countdown>`
Start or resume the Listening Party (countdown: the seconds to count down, default: 5)

### `/stop`
Cancel a current Listening Party and reset it to the beginning

### `/next <amount>`
Skip the current song in the Listening Party (amount: how many songs to skip, default: 1)

### `/previous <amount>`
Play the previous song in the Listening Party (amount: how many songs to go back to, default: 1)

### `/restart`
Restart the currently playing song

### `/pause`
Pause the current Listening Party (resume by typing /start again)

### `/link`
Print the set target link

### `/help`
Print this information as chat message

### `/totw <attachment>`
_[Experimental]_ Host a Track-of-the-Week party (attachment: the TOTW info data). **This feature is highly experimental and therefore shouldn't be used unless you know what you're doing!**

## Support
If you got any problems, [write an issue ticket on GitHub](https://github.com/Selbi182/ListeningPartyBot/issues) and I will gladly take a look at it :)

Alternatively, message me on Discord: **Selbi#7270**
