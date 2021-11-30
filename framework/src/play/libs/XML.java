package play.libs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.Key;
import java.security.Provider;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;

/**
 * XML utils
 */
public class XML {
    private static final Logger logger = LoggerFactory.getLogger(XML.class);

    public static DocumentBuilderFactory newDocumentBuilderFactory() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return dbf;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static DocumentBuilder newDocumentBuilder() {
        try {
            return newDocumentBuilderFactory().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialize to XML String
     * 
     * @param document
     *            The DOM document
     * @return The XML String
     */
    public static String serialize(Document document) {
        StringWriter writer = new StringWriter();
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(writer);
            transformer.transform(domSource, streamResult);
        } catch (TransformerException e) {
            throw new RuntimeException("Error when serializing XML document.", e);
        }
        return writer.toString();
    }

    /**
     * Parse an XML file to DOM
     * 
     * @param file
     *            The XML file
     * @return null if an error occurs during parsing.
     *
     */
    public static Document getDocument(File file) {
        try {
            return newDocumentBuilder().parse(file);
        } catch (SAXException e) {
            logger.warn("Parsing error when building Document object from xml file '{}'.", file, e);
        } catch (IOException e) {
            logger.warn("Reading error when building Document object from xml file '{}'.", file, e);
        }
        return null;
    }

    /**
     * Parse an XML string content to DOM
     * 
     * @param xml
     *            The XML string
     * @return null if an error occurs during parsing.
     */
    public static Document getDocument(String xml) {
        InputSource source = new InputSource(new StringReader(xml));
        try {
            return newDocumentBuilder().parse(source);
        } catch (SAXException e) {
            logger.warn("Parsing error when building Document object from xml data.", e);
        } catch (IOException e) {
            logger.warn("Reading error when building Document object from xml data.", e);
        }
        return null;
    }

    /**
     * Parse an XML coming from an input stream to DOM
     * 
     * @param stream
     *            The XML stream
     * @return null if an error occurs during parsing.
     */
    public static Document getDocument(InputStream stream) {
        try {
            return newDocumentBuilder().parse(stream);
        } catch (SAXException e) {
            logger.warn("Parsing error when building Document object from xml data.", e);
        } catch (IOException e) {
            logger.warn("Reading error when building Document object from xml data.", e);
        }
        return null;
    }

    /**
     * Check the xmldsig signature of the XML document.
     * 
     * @param document
     *            the document to test
     * @param publicKey
     *            the public key corresponding to the key pair the document was signed with
     * @return true if a correct signature is present, false otherwise
     */
    public static boolean validSignature(Document document, Key publicKey) {
        Node signatureNode = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
        KeySelector keySelector = KeySelector.singletonKeySelector(publicKey);

        try {
            String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
            Provider provider = (Provider) Class.forName(providerName).getDeclaredConstructor().newInstance();
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM", provider);
            DOMValidateContext valContext = new DOMValidateContext(keySelector, signatureNode);

            XMLSignature signature = fac.unmarshalXMLSignature(valContext);
            return signature.validate(valContext);
        } catch (Exception e) {
            logger.warn("Error validating an XML signature.", e);
            return false;
        }
    }

    /**
     * Sign the XML document using xmldsig.
     * 
     * @param document
     *            the document to sign; it will be modified by the method.
     * @param publicKey
     *            the public key from the key pair to sign the document.
     * @param privateKey
     *            the private key from the key pair to sign the document.
     * @return the signed document for chaining.
     */
    public static Document sign(Document document, RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        KeyInfoFactory keyInfoFactory = fac.getKeyInfoFactory();

        try {
            Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);
            SignedInfo si = fac.newSignedInfo(
                    fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));
            DOMSignContext dsc = new DOMSignContext(privateKey, document.getDocumentElement());
            KeyValue keyValue = keyInfoFactory.newKeyValue(publicKey);
            KeyInfo ki = keyInfoFactory.newKeyInfo(Collections.singletonList(keyValue));
            XMLSignature signature = fac.newXMLSignature(si, ki);
            signature.sign(dsc);
        } catch (Exception e) {
            logger.warn("Error while signing an XML document.", e);
        }

        return document;
    }

}
