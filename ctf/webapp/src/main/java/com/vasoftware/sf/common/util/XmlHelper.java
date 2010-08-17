/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

import java.net.URL;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.sun.rowset.internal.XmlErrorHandler;
import com.vasoftware.sf.common.logger.Logger;

/**
 * The <code>XmlHelper</code> provides utility methods to parse XML documents and navigate the DOM element tree.
 */

public abstract class XmlHelper {
    /* Document build factory. */
    private static javax.xml.parsers.DocumentBuilderFactory smDbf;

    /* DTD resolver to locate schemas employed by SourceForge. */
    private static DtdResolver smDtdResolver;

    /* XML error handler. */
    private static XmlErrorHandler smErrorHandler;

    /* Logger for this class. */
    private static final Logger smLogger = Logger.getLogger(XmlHelper.class);

    /* initialize static members */
    static {
        smDbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        smDbf.setValidating(true);
        smDtdResolver = new DtdResolver();
        smErrorHandler = new XmlErrorHandler();
    }

    /**
     * Returns the text within the child element with the specified tag.
     * 
     * @param elParent
     *            Parent element.
     * @param childTag
     *            Child element tag.
     * @return Child element text.
     */
    public static String getChildElementText(final Element elParent, final String childTag) {
        final Element elChild = getFirstChildOfType(elParent, childTag);

        if (elChild == null) {
            return null;
        }

        return getElementText(elChild);
    }

    /**
     * Returns the first child element with the specified tag.
     * 
     * @param elParent
     *            Parent element.
     * @param childTag
     *            Child element tag.
     * @return First child element with the specified tag.
     */
    public static Element getFirstChildOfType(final Element elParent, final String childTag) {
        for (Node childNode = elParent.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                final Element elChild = (Element) childNode;

                if (elChild.getTagName().equals(childTag)) {
                    return elChild;
                }
            }
        }

        return null;
    }

    /**
     * Returns the text within a specified element. Can be optimized for elements with a single text node (common case).
     * 
     * @param element
     *            Element whose text should be returned.
     * @return Element's text.
     */
    public static String getElementText(final Element element) {
        final StringBuffer elementText = new StringBuffer(256);

        for (Node childNode = element.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.TEXT_NODE) {
                final Text textNode = (Text) childNode;

                elementText.append(textNode.getData());
            } else if (childNode.getNodeType() == Node.CDATA_SECTION_NODE) {
                final CDATASection cdataNode = (CDATASection) childNode;

                elementText.append(cdataNode.getData());
            }
        }

        return elementText.toString().trim();
    }

    /**
     * Returns the next sibling element with the specified tag.
     * 
     * @param elSibling
     *            Previous sibling element.
     * @param childTag
     *            Child element tag.
     * @return Next sibling element with the specified tag.
     */
    public static Element getNextChildOfType(final Element elSibling, final String childTag) {
        for (Node childNode = elSibling.getNextSibling(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                final Element elChild = (Element) childNode;

                if (elChild.getTagName().equals(childTag)) {
                    return elChild;
                }
            }
        }

        return null;
    }

    /**
     * Parses the specified XML resource specified as a URL.
     * 
     * @param xmlUrl
     *            XML resource to parse.
     * @return Document object.
     * @throws XmlException
     *             Thrown when a parsing error is encountered.
     */
    public static Document parseXml(final URL xmlUrl) throws XmlException {
        try {
            // parse xml
            final DocumentBuilder docBuilder = smDbf.newDocumentBuilder();
            docBuilder.setEntityResolver(smDtdResolver);
            docBuilder.setErrorHandler(smErrorHandler);
            return docBuilder.parse(xmlUrl.toExternalForm());
        } catch (final Exception e) {
            final String externalForm = xmlUrl == null ? null : xmlUrl.toExternalForm();
            smLogger.error("Failed parsing XML URL: " + externalForm, e);
            throw new XmlException("Failed parsing XML URL: " + externalForm, e);
        }
    }
}
