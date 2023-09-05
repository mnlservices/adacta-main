package nl.defensie.adacta.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DateUtilsTest {

	@Test
	public void startBeforeEndTest() {
		assertTrue(DateUtils.isBefore("2022-01-01T00:00:00", "2022-01-02T00:00:00"));
	}
	@Test
	public void getDateMinusYearsTest() {
		String datum = DateUtils.getDateMinusYears(6);
		boolean t = datum.matches("\\d{4}-\\d{2}-\\d{2}");
		assertTrue(t);
	}
	@Test
	public void isBeforeTest() {
		boolean test = DateUtils.isBefore("2018-01-01", "2020-01-01");
		assertTrue(test);
	}
}
