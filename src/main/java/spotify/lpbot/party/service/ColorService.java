package spotify.lpbot.party.service;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import spotify.lpbot.party.data.ColorFetchResult;

@Component
public class ColorService {

  @Value("${colorfetch.url}")
  private String colorFetchUrl;

  private final ObjectMapper objectMapper;
  private final Map<String, Color> cache;

  ColorService() {
    this.objectMapper = new ObjectMapper();
    this.cache = new ConcurrentHashMap<>();
  }

  public Color getDominantColorFromImage(String artworkUrl) {
    if (cache.containsKey(artworkUrl)) {
      return cache.get(artworkUrl);
    } else {
      ColorFetchResult fromWebService = getFromWebService(artworkUrl);
      ColorFetchResult.RGB primary = fromWebService.getPrimary();
      Color asAwtColor = new Color(primary.getR(), primary.getG(), primary.getB());
      cache.put(artworkUrl, asAwtColor);
      return asAwtColor;
    }
  }

  public List<Color> getDominantColorsForMultipleImages(List<String> artworkUrls) {
    return artworkUrls.stream()
        .map(this::getDominantColorFromImage)
        .collect(Collectors.toList());
  }

  private ColorFetchResult getFromWebService(String artworkUrl) {
    try {
      String requestUri = UriComponentsBuilder.fromUriString(colorFetchUrl)
          .queryParam("url", artworkUrl)
          .queryParam("strategy", "color_thief").build().toUriString();
      String rawJson = Jsoup.connect(requestUri).ignoreContentType(true).execute().body();
      return objectMapper.readValue(rawJson, ColorFetchResult.class);
    } catch (IOException e) {
      e.printStackTrace();
      return ColorFetchResult.FALLBACK;
    }
  }
}
