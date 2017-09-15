/**********************************************************************************
 * $URL$
 * $Id$
 **********************************************************************************
 *
 * Copyright (c) 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.tool.podcasts.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import org.sakaiproject.time.cover.TimeService;
/**
 * Performs date validation respecting i18n.<br>
 * <b>Note:</b> This class does not support "hi_IN", "ja_JP_JP" and "th_TH" locales.
 */
public final class DateUtil {

	private DateUtil() {
	}

	/**
	 * Performs date validation checking like Feb 30, etc.
	 *
	 * @param date
	 *            The candidate String date.
	 * @param format
	 *            The given date-time format.
	 * @param locale
	 *            The given locale.
	 * @return TRUE - Conforms to a valid input date format string.<br>
	 *         FALSE - Does not conform.
	 */
	public static boolean isValidDate(final String date, final String format, final Locale locale) {
		try {
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern(format);
		    fmt.parse(date.toUpperCase());
		    return true;
		  } catch (Exception e) {
			  return false;
		  }
	}

	/**
	 * Converts the date string input using the FORMAT_STRING given.
	 *
	 * @param inputDate
	 *            The string that needs to be converted.
	 * @param FORMAT_STRING
	 *            The format the data needs to conform to
	 *
	 * @return Date
	 * 			The Date object containing the date passed in or null if invalid.
	 *
	 * @throws ParseException
	 * 			If not a valid date compared to FORMAT_STRING given
	 */
	public static Date convertDateString(final String inputDate, final String FORMAT_STRING, final Locale locale) throws ParseException {

		Date convertedDate = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat(FORMAT_STRING, locale);
		dateFormat.setTimeZone(TimeService.getLocalTimeZone());

		try {
			convertedDate = dateFormat.parse(inputDate);
		} catch (ParseException e) {
			// TODO: This is required until date-picker is internationalized.
			dateFormat = new SimpleDateFormat(FORMAT_STRING, Locale.ENGLISH);
			dateFormat.setTimeZone(TimeService.getLocalTimeZone());
			convertedDate = dateFormat.parse(inputDate);
		}

		return convertedDate;
	}
}
