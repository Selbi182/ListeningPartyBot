package spotify;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import spotify.api.SpotifyApiScopes;

@SpringBootApplication
public class ListeningPartyBot {
  public static void main(String[] args) {
    SpringApplication.run(ListeningPartyBot.class, args);
  }

  @Component
  public static class SpotifyBigPictureScopes implements SpotifyApiScopes {

    @Override
    public List<String> requiredScopes() {
      return List.of(
          "user-read-playback-position",
          "user-read-playback-state",
          "user-read-currently-playing",
          "user-read-private"
      );
    }
  }
}
