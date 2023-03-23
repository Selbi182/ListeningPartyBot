# Listening Party Bot
![Blahaj](https://i.imgur.com/NuEmaM9.png)

A Discord bot used to setup, organize, and host listening parties for Spotify playlists or albums! 

Use the following URL to invite the bot to your server:<br />
https://discord.com/oauth2/authorize?client_id=1062043789646118982&permissions=8590027776&scope=bot

## Basic Usage
Once the bot is invited to your server, you can manage listening parties on a per-channel basis. Here is how you do that:

1. Type `/set` and pass a URL to a Spotify album or playlist (you can get the link by right-clicking on the playlist or album and selecting _Share → Copy Album/Playlist Link_. The bot will then verify and preprocess the given playlist or album. Once everything looks good, a message will pop up, saying that everything is ready

![grafik](https://user-images.githubusercontent.com/8850085/227274717-87263c04-f8c0-4d08-a26b-e2d28705c996.png)


2. (Optional) Type `/link` to post the link and preview of the specified playlist or album to the current channel so that everyone can get ready. Or you can just click on the link provided in the first step.

![grafik](https://user-images.githubusercontent.com/8850085/227274953-ae28673a-9b24-42a5-abb8-0548e43112de.png)

3. Type `/start` to start the actual listening party. A custom countdown (in seconds) can be passed to this command. By default, it's 5 seconds.

![grafik](https://user-images.githubusercontent.com/8850085/227275051-059da934-8b85-47ee-9230-a43d0c1d7f6d.png)


And that's it! From here on out, the bot will provide detailed information about whichever song is currently playing in a synchronized manner. When possible, it will also display some additional information of the current song from last.fm's wiki:

![grafik](https://user-images.githubusercontent.com/8850085/227275284-484c228c-859f-455b-9349-e1a1a4fb98ac.png)

## Commands
Here's a full list of every command and what it does:

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

### `/nowplaying`
Print info of the current Listening Party for this channel

### `/link`
Print the set target link

### `/help`
Print this information as chat message

### `/totw <attachment>`
[Experimental] Host a Track-of-the-Week party (attachment: the TOTW info data). This feature is highly experimental and therefore shouldn't be used unless you know what you're doing!
