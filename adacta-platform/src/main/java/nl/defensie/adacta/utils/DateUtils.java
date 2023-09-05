package nl.defensie.adacta.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Util class for dates.
 * 
 * @author Rick de Rooij
 *
 */
public class DateUtils {

	/**
	 * Get the date difference.
	 * 
	 * @param date1
	 *            Date the modified, created or some other date of a node
	 * @param date2
	 *            Date the current date
	 * @param timeUnit
	 *            the time unit (days)
	 * @return Long the date difference in provided time unit.
	 */
	public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		long diffInMillies = date2.getTime() - date1.getTime();
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS); 
	}
	/**
	 * Gets a string representation of a date "years" ago.
	 * @param years
	 * @return String date representation in format yyyy-MM-dd
	 */
	public static String getDateMinusYears(int years) {
		Date d = new Date();
		LocalDate ld =  d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate ld2 = ld.minusYears(years);
		return addZeros(ld2.getYear(),4)+"-"+addZeros(ld2.getMonthValue(),2)+"-"+addZeros(ld2.getDayOfMonth(),2);
	}
	
	public static String addZeros(int s, int zeros) {
		String r = s+"";
		while (r.length()<zeros) {
			r = "0"+r;
		}
		return r;
	}
	/**
	 * Returns true if startDate lies before endDate, else False.
	 * @param startdate
	 * @param enddate
	 * @return
	 */
	public static boolean isBefore(String startDate, String endDate) {
		if (null != startDate && startDate.length()>10) {
			startDate = startDate.substring(0, 10);
		}
		if (null != endDate && endDate.length()>10) {
			endDate = endDate.substring(0, 10);
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		formatter = formatter.withLocale( Locale.GERMAN ); 
		LocalDate dateStart = LocalDate.parse(startDate, formatter);
		LocalDate dateEnd = LocalDate.parse(endDate, formatter);
		return dateStart.isBefore(dateEnd);
	}
}