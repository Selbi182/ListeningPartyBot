package spotify.lpbot.party.data.lastfm;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LastFmWikiEntry {
  private static final SimpleDateFormat PARSER_DATE = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
  private static final SimpleDateFormat FORMAT_DATE = new SimpleDateFormat("MMMM d'th', yyyy", Locale.US);

  private String published;
  private String summary;

  public LastFmWikiEntry() {
  }

  public String getPublished() {
    return published;
  }

  /**
   * Return the wiki entry publish date in the following format:
   * <pre>November 21st, 2022</pre>
   *
   * @return the formatted date, or the unformatted one if the formatting fails
   */
  @JsonIgnore
  public String getFormattedPublishDate() {
    try {
      Date parsed = PARSER_DATE.parse(published);
      Calendar cale = Calendar.getInstance();
      cale.setTime(parsed);
      int n = cale.get(Calendar.DAY_OF_MONTH);
      String suffix = "th";
      if (n < 11 || n > 13) {
        switch (n % 10) {
          case 1:
            suffix = "st";
            break;
          case 2:
            suffix = "nd";
            break;
          case 3:
            suffix = "rd";
            break;
        }
      }
      return FORMAT_DATE.format(parsed).replaceFirst("th", suffix);
    } catch (ParseException e) {
      return published;
    }
  }

  public String getSummary() {
    return summary;
  }

  public void setPublished(String published) {
    this.published = published;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }
}
