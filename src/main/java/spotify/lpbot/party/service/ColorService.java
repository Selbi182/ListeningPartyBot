package spotify.lpbot.party.service;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.selbi.colorfetch.data.ColorFetchResult;
import spotify.lpbot.party.data.color.ColorProvider;
import spotify.lpbot.party.data.color.ExternalColorProvider;
import spotify.lpbot.party.data.color.InternalColorProvider;

@Component
public class ColorService {
  @Value("${colorfetch.url:#{null}}")
  private String colorFetchServiceUrl;

  private ColorProvider colorProvider;

  private final Logger logger = Logger.getLogger(ColorService.class.getName());

  @PostConstruct
  void printColorLibraryState() {
    if (useExternalWebservice()) {
      logger.info("Using external color fetch service: " + colorFetchServiceUrl);
      this.colorProvider = new ExternalColorProvider(colorFetchServiceUrl);
    } else {
      logger.info("'colorfetch.url' not set in application.properties - using internal color fetch service");
      this.colorProvider = new InternalColorProvider();
    }
  }

  public ColorFetchResult getDominantColorFromImageUrl(String artworkUrl) {
    return colorProvider.getDominantColorFromImageUrl(artworkUrl);
  }

  private boolean useExternalWebservice() {
    return colorFetchServiceUrl != null;
  }
}
