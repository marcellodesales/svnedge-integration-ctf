package com.vasoftware.sf.externalintegration.adapters.perforcedaemon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vasoftware.sf.common.SfSystemException;
import com.vasoftware.sf.common.logger.Logger;

/**
 * Class for abstracting a Perforce text form. This parses a form as a big string, and turns it into a map: the keys are
 * the field names, and the values are single Strings (for one-line entries) or Collections of Strings (for multi-line
 * entries). The Collections are initially created as Strings.
 * 
 * See testPerforceForm() in PerforceWrapperTest for an example.
 */
@SuppressWarnings("unchecked")
public class PerforceForm {
    private static final Logger smLogger = Logger.getLogger(PerforceForm.class);

    private final Map mContents = new LinkedHashMap(); // keys are sections, values are strings or collections of

    // strings

    /**
     * Default constructor is private. Use "parse()", etc. to construct
     */
    private PerforceForm() {
    }

    /**
     * Parse the string value of a Perforce form and return a PerforceForm object that represents it. This is the normal
     * way of getting a PerforceForm object.
     * 
     * @param input
     *            the text representation of a Peforce form, such as might be retrieved by adding the "-o" option to a
     *            Perforce form-editing command
     * @return an object representing the information in the form
     */
    public static PerforceForm parse(final String input) {
        final PerforceForm form = new PerforceForm();
        form.parseInput(input);
        return form;
    }

    /**
     * Private method to parse a text string into this form object
     * 
     * @param input
     *            the form's text
     */
    private void parseInput(final String input) {
        final String[] lines = input.split("\n");

        final Pattern pattern = Pattern.compile("(\\w*):(.*)");

        List<String> currentList = null;
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            if (line.startsWith("#") || line.length() < 1 || line.trim().length() < 1) {
                continue;
            }

            final Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                final String sectionName = matcher.group(1);
                final String remaining = matcher.group(2).trim();

                if (remaining.length() < 1) {
                    // this is the beginning of a section
                    currentList = new LinkedList<String>();
                    mContents.put(sectionName, currentList);
                } else {
                    // simple key-value line
                    mContents.put(sectionName, remaining);
                }
            } else if (line.startsWith("\t")) {
                currentList.add(line.trim());
            } else {
                throw new SfSystemException("Unexpected line: " + line);
            }
        }
    }

    /**
     * Get string value from the form. This is a value defined on a single line.
     * 
     * @param key
     *            the field name to find
     * @return the string valu
     */
    public String getStringValue(final String key) {
        return (String) mContents.get(key);
    }

    /**
     * Return a list value from the form. This is a value defined on multiple lines.
     * 
     * @param key
     *            the field name to fetch
     * @return a List of Strings containing the value for the field.
     */
    public List getListValue(final String key) {
        return new ArrayList((Collection) mContents.get(key)); // clone it so we're immune to modifications
    }

    /**
     * Set a string-valued form value to a new value. This will fail if the previous value was a multi-lined value.
     * Example: "Username", "joe"
     * 
     * @param key
     *            the form field name
     * @param value
     *            the form field value
     */
    public void setStringValue(final String key, final String value) {
        final Object currentValue = mContents.get(key);
        if (currentValue == null) {
            smLogger.warn("Setting form field that wasn't there before: " + key);
        } else if (!(currentValue instanceof String)) {
            throw new SfSystemException("attempted to change type of value from list to string for key: " + key);
        }
        mContents.put(key, value);
    }

    /**
     * Set a list-valued field to the specified Collection. This will fail if the previous value was a single-line
     * string value. Example: "Members", ("joe", "fred", "sam")
     * 
     * @param key
     *            the form field name
     * @param value
     *            a collection of Strings containing the new value for the form field
     */
    public void setListValue(final String key, final Collection value) {
        final Object currentValue = mContents.get(key);
        if (currentValue == null) {
            smLogger.warn("Setting form field that wasn't there before: " + key);
        } else if (!(currentValue instanceof Collection)) {
            throw new SfSystemException("attempted to change type of value from string to list for key: " + key);
        }
        mContents.put(key, value);
    }

    /**
     * Does the form contain the specified key as a field name
     * 
     * @param key
     *            the field to look for
     * @return true iff the form contains a field of that name
     */
    public boolean containsKey(final String key) {
        return mContents.containsKey(key);
    }

    /**
     * Render the form as a string, suitable for feeding back to a Perforce "-i" command
     * 
     * @return the string value.
     */
    @Override
    public String toString() {
        final StringBuffer output = new StringBuffer();
        for (final Iterator iterator = mContents.entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry entry = (Map.Entry) iterator.next();
            if (entry.getValue() instanceof String) {
                output.append(entry.getKey());
                output.append(": ");
                output.append(entry.getValue());
                output.append("\n");
            } else {
                output.append(entry.getKey());
                output.append(":\n");
                final Collection listValue = (Collection) entry.getValue();
                for (final Iterator iter2 = listValue.iterator(); iter2.hasNext();) {
                    final String value = (String) iter2.next();
                    output.append("\t");
                    output.append(value);
                    output.append("\n");
                }
            }
        }

        return output.toString();
    }
}
