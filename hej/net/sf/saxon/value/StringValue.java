package net.sf.saxon.value;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.serialize.charcode.UTF16CharacterSet;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.UnfailingIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.*;


/**
 * An atomic value of type xs:string. This class is also used for types derived from xs:string.
 * Subclasses of StringValue are used for xs:untypedAtomic and xs:anyURI values.
 */

public class StringValue extends AtomicValue {

    public static final StringValue EMPTY_STRING = new StringValue("");
    public static final StringValue SINGLE_SPACE = new StringValue(" ");
    public static final StringValue TRUE = new StringValue("true");
    public static final StringValue FALSE = new StringValue("false");

    // We hold the value as a CharSequence (it may be a StringBuffer rather than a string)
    // But the first time this is converted to a string, we keep it as a string

    protected CharSequence value;     // may be zero-length, will never be null
    protected boolean noSurrogates = false;

    /**
     * Protected constructor for use by subtypes
     */

    protected StringValue() {
        value = "";
        typeLabel = BuiltInAtomicType.STRING;
    }

    /**
     * Constructor. Note that although a StringValue may wrap any kind of CharSequence
     * (usually a String, but it can also be, for example, a StringBuffer), the caller
     * is responsible for ensuring that the value is immutable.
     * @param value the String value. Null is taken as equivalent to "".
     */

    public StringValue(CharSequence value) {
        this.value = (value == null ? "" : value);
        typeLabel = BuiltInAtomicType.STRING;
    }

    /**
     * Assert that the string is known to contain no surrogate pairs
     */

    public void setContainsNoSurrogates() {
        noSurrogates = true;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        StringValue v = new StringValue(value);
        v.noSurrogates = noSurrogates;
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.STRING;
    }

    /**
     * Factory method. Unlike the constructor, this avoids creating a new StringValue in the case
     * of a zero-length string (and potentially other strings, in future)
     * @param value the String value. Null is taken as equivalent to "".
     * @return the corresponding StringValue
     */

    public static StringValue makeStringValue(CharSequence value) {
        if (value == null || value.length() == 0) {
            return StringValue.EMPTY_STRING;
        } else {
            return new StringValue(value);
        }
    }

    /**
     * Get the string value as a String
     */

    public final String getPrimitiveStringValue() {
        return (String) (value = value.toString());
    }

    /**
     * Set the value of the item as a CharSequence.
     * <p><b>For system use only. In principle, a StringValue is immutable. However, in special circumstances,
     * if it is newly constructed, the content can be changed to reflect the effect of the whiteSpace facet.</b></p>
     * @param value the value of the string
     */
    
    public final void setStringValueCS(CharSequence value) {
        this.value = value;
    }

    /**
     * Convert a value to another primitive data type, with control over how validation is
     * handled.
     * @param requiredType type code of the required atomic type. This must not be a namespace-sensitive type.
     * @param validate true if validation is required. If set to false, the caller guarantees that
     * the value is valid for the target data type, and that further validation is therefore not required.
     * Note that a validation failure may be reported even if validation was not requested.
     * @param rules The conversion rules to be applied
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     * will be a ValidationErrorValue. The caller must check for this condition. No exception is thrown, instead
     * the exception will be encapsulated within the ErrorValue.
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, ConversionRules rules) {
        int req = requiredType.getFingerprint();
        if (req == StandardNames.XS_STRING || req == StandardNames.XS_ANY_ATOMIC_TYPE) {
            return this;
        }
        return convertStringToBuiltInType(value, requiredType, rules);
    }

    /**
     * Convert a string value to another built-in data type, with control over how validation is
     * handled.
     * @param value the value to be converted
     * @param requiredType the required atomic type. This must not be a namespace-sensitive type.
     * @param rules
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     * will be a {@link ValidationFailure}. The caller must check for this condition. No exception is thrown, instead
     * the exception will be encapsulated within the ValidationFailure.
     */

    public static ConversionResult convertStringToBuiltInType(CharSequence value, BuiltInAtomicType requiredType,
                                                              ConversionRules rules) {
        try {
            switch (requiredType.getFingerprint()) {
                case StandardNames.XS_BOOLEAN: {
                    return BooleanValue.fromString(value);
                }
                case StandardNames.XS_NUMERIC:
                case StandardNames.XS_DOUBLE:
                    try {
                        double dbl = rules.getStringToDoubleConverter().stringToNumber(value);
                        return new DoubleValue(dbl);
                    } catch (NumberFormatException err) {
                        ValidationFailure ve = new ValidationFailure("Cannot convert string to double: " + value.toString());
                        ve.setErrorCode("FORG0001");
                        return ve;
                    }

                case StandardNames.XS_INTEGER:
                    return Int64Value.stringToInteger(value);


                case StandardNames.XS_UNSIGNED_LONG:
                case StandardNames.XS_UNSIGNED_INT:
                case StandardNames.XS_UNSIGNED_SHORT:
                case StandardNames.XS_UNSIGNED_BYTE:
                case StandardNames.XS_NON_POSITIVE_INTEGER:
                case StandardNames.XS_NEGATIVE_INTEGER:
                case StandardNames.XS_LONG:
                case StandardNames.XS_INT:
                case StandardNames.XS_SHORT:
                case StandardNames.XS_BYTE:
                case StandardNames.XS_NON_NEGATIVE_INTEGER:
                case StandardNames.XS_POSITIVE_INTEGER: {
                    ConversionResult iv = IntegerValue.stringToInteger(value);
                    if (iv instanceof ValidationFailure) {
                        // indicates that the conversion failed
                        return iv;
                    }
                    IntegerValue nv = (IntegerValue)((IntegerValue)iv).copyAsSubType(requiredType);
                    ValidationFailure err = nv.convertToSubType(requiredType, rules != null);
                    //noinspection RedundantCast
                    return (err == null ? (ConversionResult)nv : (ConversionResult)err);
                }
                case StandardNames.XS_DECIMAL:
                    return DecimalValue.makeDecimalValue(value, rules != null);
                case StandardNames.XS_PRECISION_DECIMAL:
                    return PrecisionDecimalValue.makePrecisionDecimalValue(value, rules != null);
                case StandardNames.XS_FLOAT:
                    try {
                        float flt = (float)rules.getStringToDoubleConverter().stringToNumber(value);
                        return new FloatValue(flt);
                    } catch (NumberFormatException err) {
                        ValidationFailure ve = new ValidationFailure("Cannot convert string to float: " + value.toString());
                        ve.setErrorCode("FORG0001");
                        return ve;
                    }
                case StandardNames.XS_DATE:
                    return DateValue.makeDateValue(value, rules);
                case StandardNames.XS_DATE_TIME:
                    return DateTimeValue.makeDateTimeValue(value, rules);
                case StandardNames.XS_DATE_TIME_STAMP: {
                    ConversionResult cr = DateTimeValue.makeDateTimeValue(value, rules);
                    if (cr instanceof DateTimeValue) {
                        ValidationFailure vf = ((DateTimeValue)cr).convertToSubType(requiredType);
                        if (vf != null) {
                            cr = vf;
                        }
                    }
                    return cr;
                }
                case StandardNames.XS_TIME:
                    return TimeValue.makeTimeValue(value);
                case StandardNames.XS_G_YEAR:
                    return GYearValue.makeGYearValue(value, rules);
                case StandardNames.XS_G_YEAR_MONTH:
                    return GYearMonthValue.makeGYearMonthValue(value, rules);
                case StandardNames.XS_G_MONTH:
                    return GMonthValue.makeGMonthValue(value, rules);
                case StandardNames.XS_G_MONTH_DAY:
                    return GMonthDayValue.makeGMonthDayValue(value, rules);
                case StandardNames.XS_G_DAY:
                    return GDayValue.makeGDayValue(value, rules);
                case StandardNames.XS_DURATION:
                    return DurationValue.makeDuration(value);
                case StandardNames.XS_YEAR_MONTH_DURATION:
                    return YearMonthDurationValue.makeYearMonthDurationValue(value);
                case StandardNames.XS_DAY_TIME_DURATION:
                    return DayTimeDurationValue.makeDayTimeDurationValue(value);
                case StandardNames.XS_UNTYPED_ATOMIC:
                case StandardNames.XS_ANY_SIMPLE_TYPE:
                case StandardNames.XS_ANY_ATOMIC_TYPE:
                    return new UntypedAtomicValue(value);
                case StandardNames.XS_STRING:
                    return makeStringValue(value);
                case StandardNames.XS_NORMALIZED_STRING:
                case StandardNames.XS_TOKEN:
                case StandardNames.XS_LANGUAGE:
                case StandardNames.XS_NAME:
                case StandardNames.XS_NCNAME:
                case StandardNames.XS_ID:
                case StandardNames.XS_IDREF:
                case StandardNames.XS_ENTITY:
                case StandardNames.XS_NMTOKEN:
                    return makeRestrictedString(value, requiredType, rules.getNameChecker());
                case StandardNames.XS_ANY_URI:
                    if (rules.isValidURI(value)) {
                        return new AnyURIValue(value);
                    } else {
                        ValidationFailure ve = new ValidationFailure("Invalid URI: " + value.toString());
                        ve.setErrorCode("FORG0001");
                        return ve;
                    }
                case StandardNames.XS_HEX_BINARY:
                    return new HexBinaryValue(value);
                case StandardNames.XS_BASE64_BINARY:
                    return new Base64BinaryValue(value);
                default:
                    ValidationFailure ve = new ValidationFailure("Cannot convert string to type " +
                            Err.wrap(requiredType.getDisplayName()));
                    ve.setErrorCode("XPTY0004");
                    return ve;
            }
        } catch (ValidationException err) {
            ValidationFailure vf = new ValidationFailure(err.getMessage());
            vf.setErrorCodeQName(err.getErrorCodeQName());
            if (vf.getErrorCodeQName() == null) {
                vf.setErrorCode("FORG0001");
            }
            return vf;
        } catch (XPathException err) {
            err.maybeSetErrorCode("FORG0001");
            ValidationFailure ve = new ValidationFailure(err.getMessage());
            if (err.getErrorCodeQName() == null) {
                ve.setErrorCode("FORG0001");
            } else {
                ve.setErrorCodeQName(err.getErrorCodeQName());
            }
            return ve;
        }
    }

    /**
     * Convert the value to a given type. The result of the conversion will be
     * an atomic value of the required type. This method works where the target
     * type is a built-in atomic type and also where it is a user-defined atomic
     * type. It does not handle namespace-sensitive types (QName, NOTATION, and derivatives).
     * @param value the value to be converted
     * @param targetType the type to which the value is to be converted.
     *                  This must not be a namespace-sensitive type.
     * @param rules The conversion rules to be applied
     * @return the value after conversion if successful; or a {@link ValidationFailure} if conversion failed. The
     *         caller must check for this condition. Validation may fail even if validation was not requested.
     */

    public static ConversionResult convertStringToAtomicType(
            CharSequence value, AtomicType targetType, ConversionRules rules) {
        if (targetType instanceof BuiltInAtomicType) {
            return convertStringToBuiltInType(value, (BuiltInAtomicType)targetType, rules);
        } else {
            BuiltInAtomicType primitiveType = (BuiltInAtomicType)targetType.getPrimitiveItemType();
            if (primitiveType.getFingerprint() == StandardNames.XS_STRING) {
                int whitespaceAction = targetType.getWhitespaceAction();
                value = Whitespace.applyWhitespaceNormalization(whitespaceAction, value);
            }

            try {
                value = targetType.preprocess(value);
            } catch (ValidationException err) {
                return new ValidationFailure(err);
            }

            ConversionResult result =
                    convertStringToBuiltInType(value, primitiveType, rules);
            if (result instanceof ValidationFailure) {
                // conversion has failed
                return result;
            }
            ValidationFailure vf = targetType.validate((AtomicValue)result, value, rules);
            if (vf != null) {
                return vf;
            }
            return ((AtomicValue)result).copyAsSubType(targetType);
        }
    }


    /**
     * Get the length of this string, as defined in XPath. This is not the same as the Java length,
     * as a Unicode surrogate pair counts as a single character
     * @return the length of the string in Unicode code points
     */

    public int getStringLength() {
        if (noSurrogates) {
            return value.length();
        } else {
            int len = getStringLength(value);
            if (len == value.length()) {
                noSurrogates = true;
            }
            return len;
        }
    }

    /**
     * Get the length of a string, as defined in XPath. This is not the same as the Java length,
     * as a Unicode surrogate pair counts as a single character.
     * @param s The string whose length is required
     * @return the length of the string in Unicode code points
     */

    public static int getStringLength(CharSequence s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = (int) s.charAt(i);
            if (c < 55296 || c > 56319) n++;    // don't count high surrogates, i.e. D800 to DBFF
        }
        return n;
    }


    /**
     * Determine whether the string is a zero-length string. This may
     * be more efficient than testing whether the length is equal to zero
     * @return true if the string is zero length
     */

    public boolean isZeroLength() {
        return value.length() == 0;
    }

    /**
     * Determine whether the string contains surrogate pairs
     * @return true if the string contains any non-BMP characters
     */

    public boolean containsSurrogatePairs() {
        //noinspection SimplifiableConditionalExpression
        return (noSurrogates ? false : getStringLength() != value.length());
    }

    /**
     * Ask whether the string is known to contain no surrogate pairs.
     * @return true if it is known to contain no surrogates, false if the answer is not known
     */

    public boolean isKnownToContainNoSurrogates() {
        return noSurrogates;
    }

    /**
     * Iterate over a string, returning a sequence of integers representing the Unicode code-point values
     * @return an iterator over the characters (Unicode code points) in the string
     */

    public UnfailingIterator iterateCharacters() {
        return new CharacterIterator();
    }

    /**
     * Expand a string containing surrogate pairs into an array of 32-bit characters
     * @return an array of integers representing the Unicode code points
     */

    public int[] expand() {
        int[] array = new int[getStringLength()];
        int o = 0;
        int len = value.length();
        for (int i = 0; i < len; i++) {
            int charval;
            int c = value.charAt(i);
            if (c >= 55296 && c <= 56319) {
                // we'll trust the data to be sound
                charval = ((c - 55296) * 1024) + ((int) value.charAt(i + 1) - 56320) + 65536;
                i++;
            } else {
                charval = c;
            }
            array[o++] = charval;
        }
        return array;
    }

                         
    /**
     * Expand a string containing surrogate pairs into an array of 32-bit characters
     * @param s the string to be expanded
     * @return an array of integers representing the Unicode code points
     */

    public static int[] expand(CharSequence s) {
        int[] array = new int[getStringLength(s)];
        int o = 0;
        for (int i = 0; i < s.length(); i++) {
            int charval;
            int c = s.charAt(i);
            if (c >= 55296 && c <= 56319) {
                // we'll trust the data to be sound
                charval = ((c - 55296) * 1024) + ((int) s.charAt(i + 1) - 56320) + 65536;
                i++;
            } else {
                charval = c;
            }
            array[o++] = charval;
        }
        return array;
    }

    /**
     * Contract an array of integers containing Unicode codepoints into a Java string
     * @param codes an array of integers representing the Unicode code points
     * @param used the number of items in the array that are actually used
     * @return the constructed string
     */

    public static CharSequence contract(int[] codes, int used) {
        FastStringBuffer sb = new FastStringBuffer(codes.length);
        for (int i=0; i<used; i++) {
            if (codes[i]<65536) {
                sb.append((char)codes[i]);
            }
            else {  // output a surrogate pair
                sb.append(UTF16CharacterSet.highSurrogate(codes[i]));
                sb.append(UTF16CharacterSet.lowSurrogate(codes[i]));
            }
        }
        return sb;
    }


    /**
     * Get an object value that implements the XPath equality and ordering comparison semantics for this value.
     * If the ordered parameter is set to true, the result will be a Comparable and will support a compareTo()
     * method with the semantics of the XPath lt/gt operator, provided that the other operand is also obtained
     * using the getXPathComparable() method. In all cases the result will support equals() and hashCode() methods
     * that support the semantics of the XPath eq operator, again provided that the other operand is also obtained
     * using the getXPathComparable() method. A context argument is supplied for use in cases where the comparison
     * semantics are context-sensitive, for example where they depend on the implicit timezone or the default
     * collation.
     *
     * @param ordered true if an ordered comparison is required. In this case the result is null if the
     *                type is unordered; in other cases the returned value will be a Comparable.
     * @param collator Collation to be used for comparing strings
     * @param context the XPath dynamic evaluation context, used in cases where the comparison is context
     *                sensitive
     * @return an Object whose equals() and hashCode() methods implement the XPath comparison semantics
     *         with respect to this atomic value. If ordered is specified, the result will either be null if
     *         no ordering is defined, or will be a Comparable
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
        return collator.getCollationKey(value.toString());
    }

    /**
     * Determine if two AtomicValues are equal, according to XPath rules. (This method
     * is not used for string comparisons, which are always under the control of a collation.
     * If we get here, it's because there's a type error in the comparison.)
     * @throws ClassCastException always
     */

    public boolean equals(Object other) {
        throw new ClassCastException("equals on StringValue is not allowed");
    }
    
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Test whether this StringValue is equal to another under the rules of the codepoint collation
     * @param other the value to be compared with this value
     * @return true if the strings are equal on a codepoint-by-codepoint basis
     */

    public boolean codepointEquals(StringValue other) {
        // avoid conversion of CharSequence to String if values are different lengths
        return value.length() == other.value.length() &&
                value.toString().equals(other.value.toString());
        // It might be better to do character-by-character comparison in all cases; or it might not.
        // We do it this way in the hope that string comparison compiles to native code.
    }

    /**
     * Get the effective boolean value of a string
     * @return true if the string has length greater than zero
     */

    public boolean effectiveBooleanValue() {
        return value.length() > 0;
    }


    public String toString() {
        return "\"" + value + '\"';
    }

    /**
     * Factory method to create a string value belonging to a built-in type derived from string
     * @param value the String value. Null is taken as equivalent to "".
     * @param typeLabel the required type, must be a built in type derived from xs:string
     * @param checker a NameChecker if validation is required,
     *        null if the caller already knows that the value is valid
     * @return either the required StringValue if the value is valid, or a ValidationFailure encapsulating
     * the error message if not.
     */

    public static ConversionResult makeRestrictedString(
            CharSequence value, BuiltInAtomicType typeLabel, NameChecker checker) {
        StringValue rsv = new StringValue();
        int type = typeLabel.getFingerprint();
        rsv.setTypeLabel(typeLabel);
        if (value == null) {
            rsv.value = "";
        } else if (type == StandardNames.XS_NORMALIZED_STRING) {
            rsv.value = Whitespace.normalizeWhitespace(value);
        } else if (type == StandardNames.XS_TOKEN) {
            rsv.value = Whitespace.collapseWhitespace(value);
        } else {
            rsv.value = Whitespace.trimWhitespace(value);
            if (checker != null) {
                ValidationFailure err = validate(typeLabel, rsv.value, checker);
                if (err == null) {
                    return rsv;
                } else {
                    return err;
                }
            } else {
                return rsv;
            }
        }
        return rsv;
    }

    /**
     * Validate that the string conforms to the rules for a built-in subtype of xs:string
     * @param typeLabel the built-in atomic type against which the string should be validated
     * @param val the string to be validated
     * @param checker object that checks names against the XML 1.0 or XML 1.1 rules as appropriate
     * @return null if the value is OK, otherwise a ValidationException containing details of the failure
     */

    public static ValidationFailure validate(BuiltInAtomicType typeLabel, CharSequence val, NameChecker checker) {
        switch (typeLabel.getFingerprint()) {

            case StandardNames.XS_TOKEN:
                return null;
            case StandardNames.XS_NORMALIZED_STRING:
                return null;
            case StandardNames.XS_LANGUAGE:
                String regex = "[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*";
                            // See erratum E2-25 to XML Schema Part 2. Was:
//                        "(([a-z]|[A-Z])([a-z]|[A-Z])|" // ISO639Code
//                        + "([iI]-([a-z]|[A-Z])+)|"     // IanaCode
//                        + "([xX]-([a-z]|[A-Z])+))"     // UserCode
//                        + "(-([a-z]|[A-Z])+)*";        // Subcode
                if (!java.util.regex.Pattern.matches(regex, val.toString())) {
                    ValidationFailure err = new ValidationFailure("The value '" + val + "' is not a valid xs:language");
                    err.setErrorCode("FORG0001");
                    return err;
                }
                return null;
            case StandardNames.XS_NAME:
                // replace any colons by underscores and then test if it's a valid NCName
                FastStringBuffer buff = new FastStringBuffer(val.length());
                buff.append(val);
                for (int i = 0; i < buff.length(); i++) {
                    if (buff.charAt(i) == ':') {
                        buff.setCharAt(i, '_');
                    }
                }
                if (!checker.isValidNCName(buff)) {
                    ValidationFailure err = new ValidationFailure(
                            "The value " + Err.wrap(val, Err.VALUE) + " is not a valid Name");
                    err.setErrorCode("FORG0001");
                    return err;
                }
                return null;
            case StandardNames.XS_NCNAME:
            case StandardNames.XS_ID:
            case StandardNames.XS_IDREF:
            case StandardNames.XS_ENTITY:
                if (!checker.isValidNCName(val)) {
                    ValidationFailure err = new ValidationFailure(
                            "The value " + Err.wrap(val, Err.VALUE) + " is not a valid NCName");
                    err.setErrorCode("FORG0001");
                    return err;
                }
                return null;
            case StandardNames.XS_NMTOKEN:
                if (!checker.isValidNmtoken(val)) {
                    ValidationFailure err = new ValidationFailure(
                            "The value " + Err.wrap(val, Err.VALUE) + " is not a valid NMTOKEN");
                    err.setErrorCode("FORG0001");
                    return err;
                }
                return null;
            default:
                throw new IllegalArgumentException("Unknown string value type " + typeLabel.getFingerprint());
        }
    }

    /**
     * Get a Comparable value that implements the XML Schema comparison semantics for this value.
     * Returns null if the value is not comparable according to XML Schema rules. This implementation
     * returns the underlying Java string, which works because strings will only be compared for
     * equality, not for ordering, and the equality rules for strings in XML schema are the same as in Java.
     */

    public Comparable getSchemaComparable() {
        return value.toString();
    }

    /**
     * Determine whether two atomic values are identical, as determined by XML Schema rules. This is a stronger
     * test than equality (even schema-equality); for example two dateTime values are not identical unless
     * they are in the same timezone.
     * <p>Note that even this check ignores the type annotation of the value. The integer 3 and the short 3
     * are considered identical, even though they are not fully interchangeable. "Identical" means the
     * same point in the value space, regardless of type annotation.</p>
     * <p>NaN is identical to itself.</p>
     * @param v the other value to be compared with this one
     * @return true if the two values are identical, false otherwise.
     */

    public boolean isIdentical(Value v) {
        return v instanceof StringValue &&
                (this instanceof AnyURIValue == v instanceof AnyURIValue) &&
                (this instanceof UntypedAtomicValue == v instanceof UntypedAtomicValue) &&
                toString().equals(v.toString());
    }

    /**
     * Produce a diagnostic representation of the contents of the string
     * @param s the string
     * @return a string in which non-Ascii-printable characters are replaced by \ uXXXX escapes
     */

    public static String diagnosticDisplay(String s) {
        FastStringBuffer fsb = new FastStringBuffer(s.length());
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c <= 0x7e) {
                fsb.append(c);
            } else {
                fsb.append("\\u");
                for (int shift = 12; shift >= 0; shift -= 4) {
                    fsb.append("0123456789ABCDEF".charAt((c >> shift) & 0xF));
                }
            }
        }
        return fsb.toString();
    }

    /**
     * CharacterIterator is used to iterate over the characters in a string,
     * returning them as integers representing the Unicode code-point.
     */


    public final class CharacterIterator implements UnfailingIterator {

        int inpos = 0;        // 0-based index of the current Java char
        int outpos = 0;       // 1-based value of position() function
        int current = -1;     // Unicode codepoint most recently returned

        /**
         * Create an iterator over a string
         */

        public CharacterIterator() {
        }

        public Item next() {
            if (inpos < value.length()) {
                int c = value.charAt(inpos++);
                if (c >= 55296 && c <= 56319) {
                    // we'll trust the data to be sound
                    try {
                        current = ((c - 55296) * 1024) + ((int) value.charAt(inpos++) - 56320) + 65536;
                    } catch (StringIndexOutOfBoundsException e) {
                        System.err.println("Invalid surrogate at end of string");
                        System.err.println(diagnosticDisplay(value.toString()));
                        e.printStackTrace();
                        throw e;
                    }
                } else {
                    current = c;
                }
                outpos++;
                return new Int64Value(current);
            } else {
                outpos = -1;
                return null;
            }
        }

        public Item current() {
            if (outpos < 1) {
                return null;
            }
            return new Int64Value(current);
        }

        public int position() {
            return outpos;
        }

        public void close() {
        }

        public SequenceIterator getAnother() {
            return new CharacterIterator();
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED} and {@link #LAST_POSITION_FINDER}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         */

        public int getProperties() {
            return 0;
        }
    }


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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
