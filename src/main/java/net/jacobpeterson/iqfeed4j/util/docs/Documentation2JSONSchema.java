package net.jacobpeterson.iqfeed4j.util.docs;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * {@link Documentation2JSONSchema} is not meant for regular runtime use. It's used to generate jsonschema2POJO schema
 * JSON from the HTML in the docs.
 */
public final class Documentation2JSONSchema {

    /**
     * This is a standalone helper method that converts properties HTML copied from the IQFeed docs page into a JSON
     * schema property list that includes the type and the description. It's kinda hacky, but it helps in the generation
     * of JSON POJOs with Javadocs.
     *
     * @param docHTML the documentation HTML
     */
    public static void printIQFeedHTMLWebDocAsJSONSchema(String docHTML) throws Exception {
        // For position(): https://stackoverflow.com/questions/2407781/get-nth-child-of-a-node-using-xpath
        // It is 1-indexed.

        String nameXPath = "//tbody/tr/td[position()=2]";
        String typeXPath = "//tbody/tr/td[position()=3]";
        String descriptionXPath = "//tbody/tr/td[position()=4]";

        System.out.println(getSchemaFromDoc(docHTML, nameXPath, typeXPath, descriptionXPath));
    }

    /**
     * Converts a generic HTML doc with "name", "type", and "description/note" into schema JSON used for
     * <a href="https://github.com/joelittlejohn/jsonschema2pojo/wiki/Reference">jsonSchema2POJO</a>.
     *
     * @param docHTML          the doc html
     * @param nameXPath        the name Xpath
     * @param typeXPath        the type Xpath
     * @param descriptionXPath the description Xpath
     *
     * @return the schema from the doc
     *
     * @throws ParserConfigurationException thrown for {@link ParserConfigurationException}s
     * @throws XPathExpressionException     thrown for {@link XPathExpressionException}s
     * @throws IOException                  thrown for {@link IOException}s
     * @throws SAXException                 thrown for {@link SAXException}s
     */
    private static String getSchemaFromDoc(String docHTML, String nameXPath, String typeXPath, String descriptionXPath)
            throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        StringBuilder schemaJSON = new StringBuilder();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(docHTML.getBytes()));
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        NodeList propertyNameNodes = (NodeList) xPath.compile(nameXPath).evaluate(document, XPathConstants.NODESET);
        NodeList propertyTypesNodes = (NodeList) xPath.compile(typeXPath).evaluate(document, XPathConstants.NODESET);
        NodeList propertyDescriptionsNodes = (NodeList) xPath.compile(descriptionXPath).evaluate(document,
                XPathConstants.NODESET);

        for (int propertyIndex = 0; propertyIndex < propertyNameNodes.getLength(); propertyIndex++) {
            String propertyName = propertyNameNodes.item(propertyIndex).getTextContent();
            String propertyType = propertyTypesNodes.item(propertyIndex).getTextContent();
            String propertyDescription = propertyDescriptionsNodes.item(propertyIndex).getTextContent();

            // Property description may be null
            if (propertyDescription == null || propertyDescription.isEmpty()) {
                propertyDescription = propertyName;
            }

            // Lower Camel Case
            propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);

            schemaJSON.append("\"").append(propertyName).append("\": {\n");
            schemaJSON.append("\"existingJavaType\": \"")
                    .append(parseDocTypeToSchemaJavaType(propertyType)).append("\",\n");
            schemaJSON.append("\"title\": \"").append(propertyDescription).append("\"\n");
            schemaJSON.append("},\n");
        }

        return schemaJSON.toString();
    }

    /**
     * @see <a href="https://github.com/joelittlejohn/jsonschema2pojo/wiki/Reference#type">jsonSchema2Pojo Reference</a>
     */
    private static String parseDocTypeToSchemaJavaType(String docType) {
        docType = docType.toLowerCase();

        if (docType.contains("string") || docType.contains("timestamp")) {
            return "java.lang.String";
        } else if (docType.contains("boolean") || docType.contains("bool")) {
            return "java.lang.Boolean";
        } else if (docType.contains("int") || docType.contains("integer")) {
            return "java.lang.Integer";
        } else if (docType.contains("double") || docType.contains("float") || docType.contains("decimal")) {
            return "java.lang.Double";
        } else {
            return "UNKNOWN DOC DATA TYPE";
        }
    }
}
