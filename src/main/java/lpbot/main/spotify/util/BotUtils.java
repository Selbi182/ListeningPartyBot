package lpbot.main.spotify.util;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.michaelthelin.spotify.enums.AlbumGroup;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

public final class BotUtils {

	/**
	 * Utility class
	 */
	private BotUtils() {
	}

	///////

	/**
	 * Performs a <code>Thread.sleep(sleepMs);</code> call in a surrounded try-catch
	 * that ignores any interrupts. This method mostly exists to reduce the number
	 * of try-catch and throws clutter throughout the code. Yes, I know it's bad
	 * practice, cry me a river.
	 * 
	 * @param millis the number of milliseconds to sleep
	 */
	public static void sneakySleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check if the given old date is still within the allowed timeout window
	 * 
	 * @param baseDate       the date to check "now" against
	 * @param timeoutInHours the timeout in hours
	 */
	public static boolean isWithinTimeoutWindow(Date baseDate, int timeoutInHours) {
		Instant baseTime = Instant.ofEpochMilli(baseDate.getTime());
		Instant currentTime = Instant.now();
		return currentTime.minus(timeoutInHours, ChronoUnit.HOURS).isBefore(baseTime);
	}

	/**
	 * Creates a map with a full AlbumGroup -> List<T> relationship (the lists are
	 * empty)
	 */
	public static <T> Map<AlbumGroup, List<T>> createAlbumGroupToListOfTMap() {
		Map<AlbumGroup, List<T>> albumGroupToList = new HashMap<>();
		for (AlbumGroup ag : AlbumGroup.values()) {
			albumGroupToList.put(ag, new ArrayList<>());
		}
		return albumGroupToList;
	}

	/**
	 * Returns true if all mappings just contain an empty list (not null)
	 */
	public static <T, K> boolean isAllEmptyLists(Map<K, List<T>> listsByMap) {
		return listsByMap.values().stream().allMatch(List::isEmpty);
	}

	/**
	 * Remove all items from this list that are either <i>null</i> or "null" (a
	 * literal String)
	 */
	public static void removeNullStrings(Collection<String> collection) {
		collection.removeIf(e -> e == null || e.equalsIgnoreCase("null"));
	}

	/**
	 * Remove all items from this collection that are null
	 */
	public static void removeNulls(Collection<?> collection) {
		collection.removeIf(Objects::isNull);
	}

	/**
	 * Return the current time as unix timestamp
	 */
	public static long currentTime() {
		Calendar cal = Calendar.getInstance();
		return cal.getTimeInMillis();
	}

	/**
	 * Build a readable String for an AlbumSimplified
	 */
	public static String formatAlbum(AlbumSimplified as) {
		return String.format("[%s] %s - %s (%s)",
			as.getAlbumGroup().toString(),
			joinArtists(as.getArtists()),
			as.getName(),
			as.getReleaseDate());
	}
	
	/**
	 * Build a readable String for a LastFmTrack
	 */
	public static String formatTrack(Track t) {
		return String.format("%s - %s",
			joinArtists(t.getArtists()),
			t.getName());
	}

	/**
	 * Return a string representation of all artist names, separated by ", "
	 */
	public static String joinArtists(ArtistSimplified[] artists) {
		return Stream.of(artists)
			.map(ArtistSimplified::getName)
			.collect(Collectors.joining(", "));
	}

	/**
	 * Returns the name of the first artist of this album (usually the only one)
	 */
	public static String getFirstArtistName(AlbumSimplified as) {
		return as.getArtists()[0].getName();
	}

	/**
	 * Returns the name of the last artist of this album
	 */
	public static String getLastArtistName(AlbumSimplified as) {
		return as.getArtists()[as.getArtists().length - 1].getName();
	}

	/**
	 * Normalizes a file by converting it to a Path object, calling .normalize(),
	 * and returning it back as file.
	 */
	public static File normalizeFile(File file) {
		if (file != null) {
			return file.toPath().normalize().toFile();
		}
		return null;
	}

	/**
	 * Adds all the items of the given (primitive) array to the specified List, if
	 * and only if the item array is not null and contains at least one item.
	 * 
	 * @param <T>    the shared class type
	 * @param source the items to add
	 * @param target the target list
	 */
	public static <T> void addToListIfNotBlank(T[] source, List<T> target) {
		if (source != null && source.length > 0) {
			List<T> asList = Arrays.asList(source);
			target.addAll(asList);
		}
	}

	/**
	 * Find the release year of the track (which is in ISO format, so it's always the first four characters)
	 */
	public static String findReleaseYear(Track track) {
		if (track.getAlbum().getReleaseDate() != null) {
			return track.getAlbum().getReleaseDate().substring(0, 4);
		}
		return null;
	}

	/**
	 * Format the given time in milliseconds as mm:ss
	 */
	public static String formatTime(long timeInMs) {
		Duration duration = Duration.ofMillis(timeInMs);
		long hours = duration.toHours();
		int minutesPart = duration.toMinutesPart();
		if (hours > 0) {
			return String.format("%d:%02d", hours, minutesPart);
		} else {
			int secondsPart = duration.toSecondsPart();
			return String.format("%d:%02d", minutesPart, secondsPart);
		}
	}
}
