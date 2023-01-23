package lpbot.main.discord.util;

import org.javacord.api.entity.message.MessageAuthor;

public final class SpamProtector {

	// TODO for now only I got the power lol
	private final static long SELBI_ID = 186507215807447041L;

	public static boolean checkAuthorOkay(MessageAuthor messageAuthor) {
		return (messageAuthor.getId() == SELBI_ID);
	}
}
