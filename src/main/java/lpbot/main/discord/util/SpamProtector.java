package lpbot.main.discord.util;

import java.util.HashMap;
import java.util.Map;

import org.javacord.api.entity.message.MessageAuthor;

public final class SpamProtector {

	private final static Map<String, Long> coolDownPerUser = new HashMap<>();
	private final static int COOL_DOWN = 1000;

	public static boolean checkAuthorOkay(MessageAuthor messageAuthor) {
		// Don't listen to bot-written messages
		if (messageAuthor.isBotUser()) {
			return false;
		}

		// Prevent spam
		String id = String.valueOf(messageAuthor.getId());
		if (coolDownPerUser.containsKey(id)) {
			if ((System.currentTimeMillis() - coolDownPerUser.get(id) - COOL_DOWN) < 0) {
				return false;
			}
		}
		coolDownPerUser.put(id, System.currentTimeMillis());
		return true;
	}
}
