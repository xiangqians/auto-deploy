package org.net.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * {@link org.springframework.util.PropertyPlaceholderHelper}
 * <p>
 * 或者使用 commons-text包下的 {@link org.apache.commons.text.StringSubstitutor} 来解析。
 *
 * @author xiangqian
 * @date 03:35 2022/07/24
 */
@Slf4j
public class PropertyPlaceholderHelper {
    private static final Map<String, String> wellKnownSimplePrefixes = new HashMap(4);
    private final String placeholderPrefix;
    private final String placeholderSuffix;
    private final String simplePrefix;
    private final String valueSeparator;
    private final boolean ignoreUnresolvablePlaceholders;

    public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
        this(placeholderPrefix, placeholderSuffix, (String) null, true);
    }

    public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix, String valueSeparator, boolean ignoreUnresolvablePlaceholders) {
        Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
        Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        String simplePrefixForSuffix = (String) wellKnownSimplePrefixes.get(this.placeholderSuffix);
        if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
            this.simplePrefix = simplePrefixForSuffix;
        } else {
            this.simplePrefix = this.placeholderPrefix;
        }

        this.valueSeparator = valueSeparator;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    public String replacePlaceholders(String value, final Properties properties) {
        Assert.notNull(properties, "'properties' must not be null");
        properties.getClass();
        return this.replacePlaceholders(value, properties::getProperty);
    }

    public String replacePlaceholders(String value, PropertyPlaceholderHelper.PlaceholderResolver placeholderResolver) {
        Assert.notNull(value, "'value' must not be null");
        return this.parseStringValue(value, placeholderResolver, null);
    }

    protected String parseStringValue(String value, PropertyPlaceholderHelper.PlaceholderResolver placeholderResolver, Set<String> visitedPlaceholders) {
        int startIndex = value.indexOf(this.placeholderPrefix);
        if (startIndex == -1) {
            return value;
        } else {
            StringBuilder result = new StringBuilder(value);

            while (startIndex != -1) {
                int endIndex = this.findPlaceholderEndIndex(result, startIndex);
                if (endIndex != -1) {
                    String placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
                    String originalPlaceholder = placeholder;
                    if (visitedPlaceholders == null) {
                        visitedPlaceholders = new HashSet(4);
                    }

                    if (!((Set) visitedPlaceholders).add(placeholder)) {
                        throw new IllegalArgumentException("Circular placeholder reference '" + placeholder + "' in property definitions");
                    }

                    placeholder = this.parseStringValue(placeholder, placeholderResolver, (Set) visitedPlaceholders);
                    String propVal = placeholderResolver.resolvePlaceholder(placeholder);
                    if (propVal == null && this.valueSeparator != null) {
                        int separatorIndex = placeholder.indexOf(this.valueSeparator);
                        if (separatorIndex != -1) {
                            String actualPlaceholder = placeholder.substring(0, separatorIndex);
                            String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
                            propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
                            if (propVal == null) {
                                propVal = defaultValue;
                            }
                        }
                    }

                    if (propVal != null) {
                        propVal = this.parseStringValue(propVal, placeholderResolver, (Set) visitedPlaceholders);
                        result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
                        if (log.isTraceEnabled()) {
                            log.trace("Resolved placeholder '" + placeholder + "'");
                        }

                        startIndex = result.indexOf(this.placeholderPrefix, startIndex + propVal.length());
                    } else {
                        if (!this.ignoreUnresolvablePlaceholders) {
                            throw new IllegalArgumentException("Could not resolve placeholder '" + placeholder + "' in value \"" + value + "\"");
                        }

                        startIndex = result.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
                    }

                    ((Set) visitedPlaceholders).remove(originalPlaceholder);
                } else {
                    startIndex = -1;
                }
            }

            return result.toString();
        }
    }

    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + this.placeholderPrefix.length();
        int withinNestedPlaceholder = 0;

        while (index < buf.length()) {
            if (substringMatch(buf, index, this.placeholderSuffix)) {
                if (withinNestedPlaceholder <= 0) {
                    return index;
                }

                --withinNestedPlaceholder;
                index += this.placeholderSuffix.length();
            } else if (substringMatch(buf, index, this.simplePrefix)) {
                ++withinNestedPlaceholder;
                index += this.simplePrefix.length();
            } else {
                ++index;
            }
        }

        return -1;
    }

    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        if (index + substring.length() > str.length()) {
            return false;
        } else {
            for (int i = 0; i < substring.length(); ++i) {
                if (str.charAt(index + i) != substring.charAt(i)) {
                    return false;
                }
            }

            return true;
        }
    }

    static {
        wellKnownSimplePrefixes.put("}", "{");
        wellKnownSimplePrefixes.put("]", "[");
        wellKnownSimplePrefixes.put(")", "(");
    }

    @FunctionalInterface
    public interface PlaceholderResolver {

        String resolvePlaceholder(String placeholderName);
    }

}
