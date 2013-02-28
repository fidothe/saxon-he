package net.sf.saxon.lib;

import net.sf.saxon.expr.number.NumericGroupFormatter;

import java.io.Serializable;

/**
  * Interface Numberer supports number formatting. There is a separate
  * implementation for each language, e.g. Numberer_en for English.
  * This supports the xsl:number element
  * @author Michael H. Kay
  */

public interface Numberer extends Serializable {

    /**
     * Set the country used by this numberer (currently used only for names of timezones).
     * <p>Note: this method is called by the system when allocating a numberer for
     * a specific language and country. Since numberers are normally shared across threads,
     * it should not be changed after the initial creation of the Numberer.</p>
     * @param country The ISO two-letter country code.
     */

    public void setCountry(String country);

    /**
     * Get the country used by this numberer
     * @return The ISO code of the country
     */

    public String getCountry();

    /**
    * Format a number into a string
    * @param number The number to be formatted
    * @param picture The format token. This is a single component of the format attribute
    * of xsl:number, e.g. "1", "01", "i", or "a"
    * @param groupSize number of digits per group (0 implies no grouping)
    * @param groupSeparator string to appear between groups of digits
    * @param letterValue The letter-value specified to xsl:number: "alphabetic" or
    * "traditional". Can also be an empty string or null.
    * @param ordinal The value of the ordinal attribute specified to xsl:number
    * The value "yes" indicates that ordinal numbers should be used; "" or null indicates
    * that cardinal numbers
    * @return the formatted number. Note that no errors are reported; if the request
    * is invalid, the number is formatted as if the string() function were used.
    */

    public String format(long number,
                         String picture,
                         int groupSize,
                         String groupSeparator,
                         String letterValue,
                         String ordinal);

    /**
    * Format a number into a string
    * @param number The number to be formatted
    * @param picture The format token. This is a single component of the format attribute
    * of xsl:number, e.g. "1", "01", "i", or "a"
    * @param numGrpFormatter an object that handles insertion of grouping separators into the formatted number
    * @param letterValue The letter-value specified to xsl:number: "alphabetic" or
    * "traditional". Can also be an empty string or null.
    * @param ordinal The value of the ordinal attribute specified to xsl:number
    * The value "yes" indicates that ordinal numbers should be used; "" or null indicates
    * that cardinal numbers
    * @return the formatted number. Note that no errors are reported; if the request
    * is invalid, the number is formatted as if the string() function were used.
    */

    public String format(long number,
            String picture,
            NumericGroupFormatter numGrpFormatter,
            String letterValue,
            String ordinal);

    /**
     * Get a month name or abbreviation
     * @param month The month number (1=January, 12=December)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     * @return the month name or abbreviation as a string (for example, "September" or "Sep")
     */

    public String monthName(int month, int minWidth, int maxWidth);

    /**
     * Get a day name or abbreviation
     * @param day The month number (1=Monday, 7=Sunday)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     * @return the day name or abbreviation as a string (for example, "Monday" or "Mon")
     */

    public String dayName(int day, int minWidth, int maxWidth);

    /**
     * Get an am/pm indicator
     * @param minutes the minutes within the day
     * @param minWidth minimum width of output
     * @param maxWidth maximum width of output
     * @return the AM or PM indicator
     */

    public String halfDayName(int minutes,
                              int minWidth, int maxWidth);

    /**
     * Get an ordinal suffix for a particular component of a date/time.
     * @param component the component specifier from a format-dateTime picture, for
     * example "M" for the month or "D" for the day.
     * @return a string that is acceptable in the ordinal attribute of xsl:number
     * to achieve the required ordinal representation. For example, "-e" for the day component
     * in German, to have the day represented as "dritte August".
     */

    public String getOrdinalSuffixForDateTime(String component);

    /**
     * Get the name for an era (e.g. "BC" or "AD")
     * @param year the proleptic gregorian year, using "0" for the year before 1AD
     * @return the name of the era, for example "AD"
     */

    public String getEraName(int year);

    /**
     * Get the name of a calendar
     * @param code The code representing the calendar as in the XSLT 2.0 spec, e.g. AD for the Gregorian calendar
     * @return the name of the calendar, for example "AD"
     */

    public String getCalendarName(String code);


}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//