package xtvapps.core.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import xtvapps.core.Utils;

public abstract class SimpleXML {
	
	private SimpleXML() {}
	
	public static Document parse(InputStream is) throws ParserException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			throw new ParserException("Error parsing XML", e);
		}
	}

	public static Document parse(File f) throws ParserException, IOException {
		FileInputStream fis = new FileInputStream(f);
		return parse(fis);
	}

	public static Document parse(byte b[]) throws ParserException {
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		return parse(bais);
	}

	// very very very simple tree traversal
	public static List<Element> getElementsXpath(Element node, String path) {
		int p = path.indexOf("/");
		if (p < 0) {
			return getElements(node, path);
		} else {
			String root = path.substring(0, p);
			String tail = path.substring(p+1);
			
			Element element = getElement(node, root);
			if (element != null) {
				return getElementsXpath(element, tail);
			}
			return null;
		}
	}
	
	public static Element getElementXpath(Element node, String path) {
		List<Element> elements = getElementsXpath(node, path);
		if (elements.size()==0) return null;
		return elements.get(0);
	}

	public static List<Element> getElements(Element node) {
		return getElements(node, null);
	}

	public static List<Element> getElements(Element node, String name) {
		List<Element> elements = new ArrayList<Element>();
		if (node == null) return elements;
		
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if ((name == null || childNode.getNodeName().equals(name)) && childNode instanceof Element) {
				elements.add((Element) childNode);
			}
		}
		return elements;
	}
	
	public static Element getElement(Element xml, String name) {
		List<Element> elements = getElements(xml, name);
		if (elements.size()==0) return null;
		return elements.get(0);
	}

	public static String getText(Node xml) {
		return getElementValue(xml, null);
	}

	
	public static String getText(Element xml, String name) {
		return getText(xml, name, null);
	}

	public static String getText(Element xml, String name, String defaultValue) {
		List<Element> elements = getElements(xml, name);

		if (elements.size() > 0)
			return getElementValue(elements.get(0), defaultValue);
		return defaultValue;
	}
	
	private static String getElementValue(Node elem, String defaultValue) {
        Node child;
        if( elem != null && elem.hasChildNodes()){
            for( child = elem.getFirstChild(); child != null; child = child.getNextSibling() ){
                if( child.getNodeType() == Node.TEXT_NODE  ){
                    return child.getNodeValue();
                }
            }
        }
        return defaultValue;
 }    
	
	public static boolean getBoolean(Element xml, String name, boolean defaultValue) {
		String sValue = defaultValue?"true":"false";
		sValue = getText(xml, name, sValue);
		return sValue!=null && sValue.equalsIgnoreCase("true");
	}

	public static String getAttribute(Node node, String name) {
		return getAttribute(node, name, null);
	}
	
	public static String getAttribute(Node node, String name, String defaultValue) {
		if (node == null) return defaultValue;
		
		NamedNodeMap attributes = node.getAttributes();
		Node namedItem = attributes.getNamedItem(name);
		if (namedItem != null)
			return namedItem.getNodeValue();
		return defaultValue;
	}

	public static int getIntAttribute(Element xml, String name, int defaultValue) {
		if (xml == null) return defaultValue;
		
		NamedNodeMap attributes = xml.getAttributes();
		Node namedItem = attributes.getNamedItem(name);
		if (namedItem == null) return defaultValue;
		return Utils.str2i(namedItem.getNodeValue(), defaultValue);
	}

	public static float getFloatAttribute(Element xml, String name) {
		return getFloatAttribute(xml, name, 0);
	}

	public static float getFloatAttribute(Element xml, String name, float defaultValue) {
		if (xml == null) return defaultValue;
		
		NamedNodeMap attributes = xml.getAttributes();
		Node namedItem = attributes.getNamedItem(name);
		if (namedItem == null) return defaultValue;
		return Utils.str2f(namedItem.getNodeValue(), defaultValue);
	}
	
	public static boolean getBoolAttribute(Node node, String name, boolean defaultValue) {
		if (node == null) return defaultValue;
		
		NamedNodeMap attributes = node.getAttributes();
		Node namedItem = attributes.getNamedItem(name);
		if (namedItem != null) {
			String sValue = namedItem.getNodeValue();
			return sValue!=null && sValue.equalsIgnoreCase("true");
		}
		return defaultValue;
	}
	

	public static int getInt(Element node, String name) {
		return getInt(node, name, 0);
	}

	public static int getInt(Element node, String name, int defaultValue) {
		String value = getText(node, name);
		return Utils.str2i(value, defaultValue);
	}

	public static int getHex(Element node, String name, int defaultValue) {
		String value = getText(node, name);
		return Utils.strHex2i(value, defaultValue);
	}
	
	public static float getFloat(Element node, String name) {
		return getFloat(node, name, 0);
	}

	public static float getFloat(Element node, String name, float defaultValue) {
		String value = getText(node, name);
		return Utils.str2f(value, defaultValue);
	}
	
	public static String asString(Element node) {
		DOMSource domSource = new DOMSource(node);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		try {
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return e.toString();
		}
	}

}
