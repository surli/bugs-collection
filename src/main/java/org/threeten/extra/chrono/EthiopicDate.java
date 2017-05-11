/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.threeten.extra.chrono;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoPeriod;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;

/**
 * A date in the Ethiopic calendar system.
 * <p>
 * This date operates using the {@linkplain EthiopicChronology Ethiopic calendar}.
 * This calendar system is primarily used in Ethiopia.
 * Dates are aligned such that {@code 0001-01-01 (Ethiopic)} is {@code 0008-08-27 (ISO)}.
 *
 * <h3>Implementation Requirements</h3>
 * This class is immutable and thread-safe.
 * <p>
 * This class must be treated as a value type. Do not synchronize, rely on the
 * identity hash code or use the distinction between equals() and ==.
 */
public final class EthiopicDate
        extends AbstractNileDate
        implements ChronoLocalDate, Serializable {
    // TODO: Check epoch day
    // TODO: Check conversion year (and Coptic)

    /**
     * Serialization version.
     */
    private static final long serialVersionUID = -268768729L;
    /**
     * The difference between the ISO and Ethiopic epoch day count.
     */
    private static final int EPOCH_DAY_DIFFERENCE = 716367;

    /**
     * The proleptic year.
     */
    private final int prolepticYear;
    /**
     * The month.
     */
    private final short month;
    /**
     * The day.
     */
    private final short day;

    //-----------------------------------------------------------------------
    /**
     * Obtains the current {@code EthiopicDate} from the system clock in the default time-zone.
     * <p>
     * This will query the {@link Clock#systemDefaultZone() system clock} in the default
     * time-zone to obtain the current date.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @return the current date using the system clock and default time-zone, not null
     */
    public static EthiopicDate now() {
        return now(Clock.systemDefaultZone());
    }

    /**
     * Obtains the current {@code EthiopicDate} from the system clock in the specified time-zone.
     * <p>
     * This will query the {@link Clock#system(ZoneId) system clock} to obtain the current date.
     * Specifying the time-zone avoids dependence on the default time-zone.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @param zone  the zone ID to use, not null
     * @return the current date using the system clock, not null
     */
    public static EthiopicDate now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    /**
     * Obtains the current {@code EthiopicDate} from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current date - today.
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@linkplain Clock dependency injection}.
     *
     * @param clock  the clock to use, not null
     * @return the current date, not null
     * @throws DateTimeException if the current date cannot be obtained
     */
    public static EthiopicDate now(Clock clock) {
        LocalDate now = LocalDate.now(clock);
        return EthiopicDate.ofEpochDay(now.toEpochDay());
    }

    /**
     * Obtains a {@code EthiopicDate} representing a date in the Ethiopic calendar
     * system from the proleptic-year, month-of-year and day-of-month fields.
     * <p>
     * This returns a {@code EthiopicDate} with the specified fields.
     * The day must be valid for the year and month, otherwise an exception will be thrown.
     *
     * @param prolepticYear  the Ethiopic proleptic-year
     * @param month  the Ethiopic month-of-year, from 1 to 13
     * @param dayOfMonth  the Ethiopic day-of-month, from 1 to 30
     * @return the date in Ethiopic calendar system, not null
     * @throws DateTimeException if the value of any field is out of range,
     *  or if the day-of-month is invalid for the month-year
     */
    public static EthiopicDate of(int prolepticYear, int month, int dayOfMonth) {
        return EthiopicDate.create(prolepticYear, month, dayOfMonth);
    }

    /**
     * Obtains a {@code EthiopicDate} from a temporal object.
     * <p>
     * This obtains a date in the Ethiopic calendar system based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code EthiopicDate}.
     * <p>
     * The conversion typically uses the {@link ChronoField#EPOCH_DAY EPOCH_DAY}
     * field, which is standardized across calendar systems.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code EthiopicDate::from}.
     *
     * @param temporal  the temporal object to convert, not null
     * @return the date in Ethiopic calendar system, not null
     * @throws DateTimeException if unable to convert to a {@code EthiopicDate}
     */
    public static EthiopicDate from(TemporalAccessor temporal) {
        if (temporal instanceof EthiopicDate) {
            return (EthiopicDate) temporal;
        }
        return EthiopicDate.ofEpochDay(temporal.getLong(EPOCH_DAY));
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains a {@code EthiopicDate} representing a date in the Ethiopic calendar
     * system from the proleptic-year and day-of-year fields.
     * <p>
     * This returns a {@code EthiopicDate} with the specified fields.
     * The day must be valid for the year, otherwise an exception will be thrown.
     *
     * @param prolepticYear  the Ethiopic proleptic-year
     * @param dayOfYear  the Ethiopic day-of-year, from 1 to 366
     * @return the date in Ethiopic calendar system, not null
     * @throws DateTimeException if the value of any field is out of range,
     *  or if the day-of-year is invalid for the year
     */
    static EthiopicDate ofYearDay(int prolepticYear, int dayOfYear) {
        EthiopicChronology.YEAR_RANGE.checkValidValue(prolepticYear, YEAR);
        DAY_OF_YEAR.range().checkValidValue(dayOfYear, DAY_OF_YEAR);
        if (dayOfYear == 366 && EthiopicChronology.INSTANCE.isLeapYear(prolepticYear) == false) {
            throw new DateTimeException("Invalid date 'Pagumen 6' as '" + prolepticYear + "' is not a leap year");
        }
        return new EthiopicDate(prolepticYear, (dayOfYear - 1) / 30 + 1, (dayOfYear - 1) % 30 + 1);
    }

    /**
     * Obtains a {@code EthiopicDate} representing a date in the Ethiopic calendar
     * system from the epoch-day.
     *
     * @param epochDay  the epoch day to convert based on 1970-01-01 (ISO)
     * @return the date in Ethiopic calendar system, not null
     * @throws DateTimeException if the epoch-day is out of range
     */
    static EthiopicDate ofEpochDay(final long epochDay) {
        EPOCH_DAY.range().checkValidValue(epochDay, EPOCH_DAY);  // validate outer bounds
        long ethiopicED = epochDay + EPOCH_DAY_DIFFERENCE;
        int adjustment = 0;
        if (ethiopicED < 0) {
            ethiopicED = ethiopicED + (1461L * (1_000_000L / 4));
            adjustment = -1_000_000;
        }
        int prolepticYear = (int) (((ethiopicED * 4) + 1463) / 1461);
        int startYearEpochDay = (prolepticYear - 1) * 365 + (prolepticYear / 4);
        int doy0 = (int) (ethiopicED - startYearEpochDay);
        int month = doy0 / 30 + 1;
        int dom = doy0 % 30 + 1;
        return new EthiopicDate(prolepticYear + adjustment, month, dom);
    }

    private static EthiopicDate resolvePreviousValid(int prolepticYear, int month, int day) {
        if (month == 13 && day > 5) {
            day = EthiopicChronology.INSTANCE.isLeapYear(prolepticYear) ? 6 : 5;
        }
        return new EthiopicDate(prolepticYear, month, day);
    }

    /**
     * Creates a {@code EthiopicDate} validating the input.
     *
     * @param prolepticYear  the Ethiopic proleptic-year
     * @param month  the Ethiopic month-of-year, from 1 to 13
     * @param dayOfMonth  the Ethiopic day-of-month, from 1 to 30
     * @return the date in Ethiopic calendar system, not null
     * @throws DateTimeException if the value of any field is out of range,
     *  or if the day-of-year is invalid for the month-year
     */
    static EthiopicDate create(int prolepticYear, int month, int dayOfMonth) {
        EthiopicChronology.YEAR_RANGE.checkValidValue(prolepticYear, YEAR);
        EthiopicChronology.MOY_RANGE.checkValidValue(month, MONTH_OF_YEAR);
        EthiopicChronology.DOM_RANGE.checkValidValue(dayOfMonth, DAY_OF_MONTH);
        if (month == 13 && dayOfMonth > 5) {
            if (EthiopicChronology.INSTANCE.isLeapYear(prolepticYear)) {
                if (dayOfMonth > 6) {
                    throw new DateTimeException("Invalid date 'Pagumen " + dayOfMonth + "', valid range from 1 to 5, or 1 to 6 in a leap year");
                }
            } else {
                if (dayOfMonth == 6) {
                    throw new DateTimeException("Invalid date 'Pagumen 6' as '" + prolepticYear + "' is not a leap year");
                } else {
                    throw new DateTimeException("Invalid date 'Pagumen " + dayOfMonth + "', valid range from 1 to 5, or 1 to 6 in a leap year");
                }
            }
        }
        return new EthiopicDate(prolepticYear, month, dayOfMonth);
    }

    //-----------------------------------------------------------------------
    /**
     * Creates an instance from validated data.
     *
     * @param prolepticYear  the Ethiopic proleptic-year
     * @param month  the Ethiopic month, from 1 to 13
     * @param dayOfMonth  the Ethiopic day-of-month, from 1 to 30
     */
    private EthiopicDate(int prolepticYear, int month, int dayOfMonth) {
        this.prolepticYear = prolepticYear;
        this.month = (short) month;
        this.day = (short) dayOfMonth;
    }

    /**
     * Validates the object.
     *
     * @return the resolved date, not null
     */
    private Object readResolve() {
        return EthiopicDate.create(prolepticYear, month, day);
    }

    //-----------------------------------------------------------------------
    @Override
    int getEpochDayDifference() {
        return EPOCH_DAY_DIFFERENCE;
    }

    @Override
    int getProlepticYear() {
        return prolepticYear;
    }

    @Override
    int getMonth() {
        return month;
    }

    @Override
    int getDayOfMonth() {
        return day;
    }

    @Override
    EthiopicDate resolvePrevious(int newYear, int newMonth, int dayOfMonth) {
        return resolvePreviousValid(newYear, newMonth, dayOfMonth);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the chronology of this date, which is the Ethiopic calendar system.
     * <p>
     * The {@code Chronology} represents the calendar system in use.
     * The era and other fields in {@link ChronoField} are defined by the chronology.
     *
     * @return the Ethiopic chronology, not null
     */
    @Override
    public EthiopicChronology getChronology() {
        return EthiopicChronology.INSTANCE;
    }

    /**
     * Gets the era applicable at this date.
     * <p>
     * The Ethiopic calendar system has two eras, 'INCARNATION' and 'BEFORE_INCARNATION',
     * defined by {@link EthiopicEra}.
     *
     * @return the era applicable at this date, not null
     */
    @Override
    public EthiopicEra getEra() {
        return (prolepticYear >= 1 ? EthiopicEra.INCARNATION : EthiopicEra.BEFORE_INCARNATION);
    }

    //-------------------------------------------------------------------------
    @Override
    public EthiopicDate with(TemporalAdjuster adjuster) {
        return (EthiopicDate) adjuster.adjustInto(this);
    }

    @Override
    public EthiopicDate with(TemporalField field, long newValue) {
        return (EthiopicDate) super.with(field, newValue);
    }

    //-----------------------------------------------------------------------
    @Override
    public EthiopicDate plus(TemporalAmount amount) {
        return (EthiopicDate) amount.addTo(this);
    }

    @Override
    public EthiopicDate plus(long amountToAdd, TemporalUnit unit) {
        return (EthiopicDate) super.plus(amountToAdd, unit);
    }

    @Override
    public EthiopicDate minus(TemporalAmount amount) {
        return (EthiopicDate) amount.subtractFrom(this);
    }

    @Override
    public EthiopicDate minus(long amountToSubtract, TemporalUnit unit) {
        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit));
    }

    //-------------------------------------------------------------------------
    @Override  // for covariant return type
    @SuppressWarnings("unchecked")
    public ChronoLocalDateTime<EthiopicDate> atTime(LocalTime localTime) {
        return (ChronoLocalDateTime<EthiopicDate>) ChronoLocalDate.super.atTime(localTime);
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        return super.until(EthiopicDate.from(endExclusive), unit);
    }

    @Override
    public ChronoPeriod until(ChronoLocalDate endDateExclusive) {
        return super.doUntil(EthiopicDate.from(endDateExclusive));
    }

}
