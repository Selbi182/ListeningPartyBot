package spotify.lpbot.party.data.color;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.selbi.colorfetch.cache.ColorCacheKey;
import de.selbi.colorfetch.data.ColorFetchResult;

@Service
public class ColorService {
  private static final String STRATEGY = ColorCacheKey.Strategy.ANDROID_PALETTE.name().toLowerCase();
  private static final float NORMALIZE = 0.65f;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Logger logger = Logger.getLogger(ColorService.class.getName());

  @Value("${color_fetch.url}")
  private String colorFetchUrl;

  @PostConstruct
  void printColorLibraryState() {
    logger.info("Using external color fetch service: " + colorFetchUrl);
  }

  public ColorFetchResult getDominantColorFromImageUrl(String artworkUrl) {
    try {
      String requestUri = UriComponentsBuilder.fromUriString(colorFetchUrl)
        .queryParam("url", artworkUrl)
        .queryParam("strategy", STRATEGY)
        .queryParam("normalize", String.valueOf(NORMALIZE))
        .build().toUriString();
      String rawJson = Jsoup.connect(requestUri).ignoreContentType(true).execute().body();
      return objectMapper.readValue(rawJson, ColorFetchResult.class);
    } catch (IOException e) {
      e.printStackTrace();
      return ColorFetchResult.FALLBACK;
    }
  }
}
