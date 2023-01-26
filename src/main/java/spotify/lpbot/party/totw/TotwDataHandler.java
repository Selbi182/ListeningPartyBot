package spotify.lpbot.party.totw;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TotwDataHandler {
  public static TotwEntity parseTextFile(String content) {
    JsonObject json = JsonParser.parseString(content).getAsJsonObject();

    String title = json.get("title").getAsString();
    JsonArray jsonSubmissions = json.get("submissions").getAsJsonArray();

    Set<String> participants = new HashSet<>();
    List<TotwEntity.Partial> submissions = new ArrayList<>();
    for (JsonElement element : jsonSubmissions) {
      JsonObject entry = element.getAsJsonObject();
      String name = entry.get("name").getAsString();
      participants.add(name);
      String lastFmName = entry.get("lastFmName").getAsString();
      String link = entry.get("link").getAsString();
      String writeUp = entry.get("writeUp").getAsString().replaceAll("\\n", "\n");
      TotwEntity.Partial partial = new TotwEntity.Partial(name, lastFmName, link, writeUp, submissions.size());
      submissions.add(partial);
    }

    return new TotwEntity(title, List.copyOf(participants), submissions);
  }
}
