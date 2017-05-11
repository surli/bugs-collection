/*
 * Copyright 2013-2015 Urs Wolfer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.urswolfer.gerrit.client.rest.gson;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Used for Gson (de-)serialization because Gerrit uses UTC as base for dates.
 *
 * I would prefer to use the default Gson parser, but I have found no way to tell Gson that dates are in UTC.
 *
 * @author Urs Wolfer
 */
public abstract class DateFormatter {
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    protected static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dateFormat;
        }
    };
}
