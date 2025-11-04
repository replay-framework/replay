/*
 * Copyright (C) 2007 by Quentin Anciaux
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *	@author Quentin Anciaux
 */
package org.allcolor.yahp.cl.converter;

import static java.lang.Double.parseDouble;
import static java.util.Collections.singletonList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.allcolor.xml.parser.dom.ADocument;
import org.allcolor.xml.parser.dom.ANode.CNamespace;
import org.allcolor.xml.parser.dom.CDom2HTMLDocument;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer.PageSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This class handle the {'https://www.allcolor.org/xmlns/yahp','pb'} tag and cut document in
 * multiples documents according to it.
 *
 * @author Quentin Anciaux
 * @version 0.94
 */
public class CDocumentCut {

  private static final Logger log = LoggerFactory.getLogger(CDocumentCut.class);
  private static final String NS = "https://www.allcolor.org/xmlns/yahp";

  record DocumentAndSize(Document doc, PageSize size) {
  }

  private static PageSize getPageSize(@Nullable String sizeAttribute, PageSize size) {
    if (sizeAttribute == null || sizeAttribute.trim().isEmpty()) {
      return size;
    }

    try {
      Field f = IHtmlToPdfTransformer.class.getDeclaredField(sizeAttribute);
      return (PageSize) f.get(null);
    } catch (Exception e) {
      log.warn("Failed to get PageSize for \"{}\"", sizeAttribute, e);
    }

    return parsePageSize(sizeAttribute).orElse(size);
  }

  private static Optional<PageSize> parsePageSize(String sizeAttribute) {
    String[] array = sizeAttribute.split(",");
    if (array.length == 2) {
      return Optional.of(new PageSize(parseDouble(array[0]), parseDouble(array[1])));
    }
    if (array.length == 3) {
      return Optional.of(new PageSize(
          parseDouble(array[0]),
          parseDouble(array[1]),
          parseDouble(array[2])
      ));
    }
    if (array.length == 6) {
      return Optional.of(new PageSize(
          parseDouble(array[0]),
          parseDouble(array[1]),
          parseDouble(array[2]),
          parseDouble(array[3]),
          parseDouble(array[4]),
          parseDouble(array[5])
      ));
    }
    log.warn("Unexpected page size attribute: \"{}\"", sizeAttribute);
    return Optional.empty();
  }

  /**
   * Cut the given document
   *
   * @param doc the document to cut
   * @return documents
   */
  static List<DocumentAndSize> cut(final Document doc, final PageSize size) {
    NodeList nl = doc.getElementsByTagNameNS(NS, "pb");

    if (nl.getLength() == 0) {
      // see if someone forgot namespace declaration
      nl = doc.getElementsByTagName("yahp:pb");
    }

    // return the given document if no page break found.
    if (nl.getLength() == 0) {
      return singletonList(new DocumentAndSize(doc, size));
    }

    // get the start-end offset of all page breaks.
    List<PbDocument> pbDocs = getPbDocs(nl);

    // create as many documents as there are pages.
    List<DocumentAndSize> result = new ArrayList<>(pbDocs.size());
    for (PbDocument pbDoc : pbDocs) {

      // get start and end offset
      Element startOffset = pbDoc.pageBreakStart();
      Element endOffset = pbDoc.pageBreakEnd();

      PageSize tmpSize = getPageSize(startOffset == null ? null : startOffset.getAttribute("size"), size);

      // create a new doc and set the URI
      ADocument newDoc = new CDom2HTMLDocument(doc.getDocumentURI());

      String prefix = endOffset != null ? endOffset.getPrefix() : startOffset.getPrefix();
      CNamespace xmlnsDef = new CNamespace(prefix, NS);
      newDoc.getNamespaceList().add(xmlnsDef);

      if (endOffset == null) {
        // from startOffset to the end of the document.

        // create the container node
        Element parentPb = (Element) newDoc.adoptNode(startOffset.getParentNode().cloneNode(false));

        // copy all next siblings
        for (Node sibling = startOffset.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
          Node node = newDoc.adoptNode(sibling.cloneNode(true));
          parentPb.appendChild(node);
        }

        // copy parent node of the start page break
        Node parent = startOffset.getParentNode();
        Node nextNode = null;
        Node ppr = null;

        while (parent != null) {
          if (parent.getNodeType() == Node.ELEMENT_NODE) {
            Node nodeCopy = newDoc.adoptNode(parent.cloneNode(false));

            if (nextNode != null) {
              // append the node to the container
              nodeCopy.appendChild(nextNode);

              if (!("body".equals(ppr.getNodeName()))) {
                // append next sibling of the previous container
                Node sibling = ppr.getNextSibling();

                while (sibling != null) {
                  Node n = newDoc.adoptNode(sibling.cloneNode(true));
                  nodeCopy.appendChild(n);
                  sibling = sibling.getNextSibling();
                }
              }

              nextNode = nodeCopy;
            }
            else {
              nextNode = parentPb;
            }

            ppr = parent;
          }

          parent = parent.getParentNode();
        }

        newDoc.appendChild(nextNode);
      }
      else {
        Element parentPb = (Element) newDoc.adoptNode(endOffset.getParentNode().cloneNode(false));
        Node sibling = endOffset.getPreviousSibling();
        boolean hasBeenDescendant = false;

        while (!hasBeenDescendant && (sibling != null)) {
          if (isDescendant(startOffset, sibling)) {
            hasBeenDescendant = true;

            break;
          }

          Node node = newDoc.adoptNode(sibling.cloneNode(true));

          if (parentPb.getChildNodes().getLength() == 0) {
            parentPb.appendChild(node);
          }
          else {
            parentPb.insertBefore(node, parentPb.getFirstChild());
          }

          sibling = sibling.getPreviousSibling();
        }

        if (hasBeenDescendant && startOffset != sibling) {
          Node c = newDoc.adoptNode(sibling.cloneNode(true));
          ((Element) c).setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:yahp", NS);
          Node pb = ((Element) c).getElementsByTagNameNS(NS, "pb").item(0);
          Node p = pb;
          Node pp = pb.getParentNode().getPreviousSibling();

          while (p != null) {
            Node tmp = p.getPreviousSibling();
            p.getParentNode().removeChild(p);
            p = tmp;
          }

          while (pp != null) {
            Node tmp = pp.getPreviousSibling();

            if (tmp == null) {
              Node ppz = pp.getParentNode();

              if (ppz.getPreviousSibling() != null) {
                tmp = ppz.getPreviousSibling();
              }
              else {
                Node prev = null;

                while (prev == null) {
                  ppz = ppz.getParentNode();

                  if (ppz == null) {
                    break;
                  }

                  prev = ppz.getPreviousSibling();
                }

                if (prev != null) {
                  tmp = prev;
                }
              }
            }

            pp.getParentNode().removeChild(pp);
            pp = tmp;
          }

          if (parentPb.getChildNodes().getLength() == 0) {
            parentPb.appendChild(c);
          }
          else {
            parentPb.insertBefore(c, parentPb.getFirstChild());
          }
        }

        Node parent = endOffset.getParentNode();
        Node previousNode = null;
        Node ppr = null;

        while (parent != null) {
          if (parent.getNodeType() == Node.ELEMENT_NODE) {
            if (previousNode != null) {
              Node node = newDoc.adoptNode(parent.cloneNode(false));
              node.appendChild(previousNode);

              if (!("body".equals(ppr.getNodeName()))) {
                sibling = ppr.getPreviousSibling();

                while (!hasBeenDescendant && (sibling != null)) {
                  if (isDescendant(startOffset, sibling)) {
                    // here special handling need to be taken
                    if (startOffset != sibling) {
                      Node c = newDoc.adoptNode(sibling.cloneNode(true));
                      Node pb = ((Element) c).getElementsByTagNameNS(NS, "pb").item(0);
                      Node p = pb;
                      Node pp = pb.getParentNode()
                          .getPreviousSibling();

                      while (p != null) {
                        Node tmp = p.getPreviousSibling();
                        p.getParentNode().removeChild(p);
                        p = tmp;
                      }

                      while (pp != null) {
                        Node tmp = pp.getPreviousSibling();

                        if (tmp == null) {
                          Node ppz = pp.getParentNode();

                          if (ppz.getPreviousSibling() != null) {
                            tmp = ppz.getPreviousSibling();
                          }
                          else {
                            Node prev = null;

                            while (prev == null) {
                              ppz = ppz.getParentNode();

                              if (ppz == null) {
                                break;
                              }

                              prev = ppz.getPreviousSibling();
                            }

                            if (prev != null) {
                              tmp = prev;
                            }
                          }
                        }

                        pp.getParentNode().removeChild(pp);
                        pp = tmp;
                      }

                      if (node.getChildNodes().getLength() == 0) {
                        node.appendChild(c);
                      }
                      else {
                        node.insertBefore(c, node.getFirstChild());
                      }
                    }

                    hasBeenDescendant = true;

                    break;
                  }

                  Node n = newDoc.adoptNode(sibling.cloneNode(true));

                  if (node.getChildNodes().getLength() == 0) {
                    node.appendChild(n);
                  }
                  else {
                    node.insertBefore(n, node.getFirstChild());
                  }

                  sibling = sibling.getPreviousSibling();
                }
              }

              previousNode = node;
            }
            else {
              previousNode = parentPb;
            }

            ppr = parent;
          }

          parent = parent.getParentNode();
        }

        newDoc.appendChild(previousNode);
      }

      copyHeader(doc, newDoc);
      result.add(new DocumentAndSize(newDoc, tmpSize));
    }

    return result;
  }

  /**
   * return true if n is a descendant of ref
   *
   * @param n   node to test
   * @param ref reference node
   * @return true if n is a descendant of ref
   */
  private static boolean isDescendant(
      @Nullable final Node n,
      @Nullable final Node ref) {
    if (ref == null) {
      return false;
    }

    if (n == ref) {
      return true;
    }

    NodeList nl = ref.getChildNodes();

    for (int i = 0; i < nl.getLength(); i++) {
      if (isDescendant(n, nl.item(i))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Return the page breaks
   *
   * @param nl page breaks nodelist
   * @return sub docs composed of space between pb.
   */
  private static List<PbDocument> getPbDocs(final NodeList nl) {
    List<PbDocument> list = new ArrayList<>();
    Element prevpb = null;

    for (int i = 0; i < nl.getLength(); i++) {
      Element pb = (Element) nl.item(i);
      PbDocument pbdoc = new PbDocument(prevpb, pb);
      list.add(pbdoc);
      prevpb = pb;

      if (i == (nl.getLength() - 1)) {
        pbdoc = new PbDocument(pb, null);
        list.add(pbdoc);
      }
    }
    return list;
  }

  /**
   * Copy the html header from doc to ndoc
   *
   * @param doc  source of html header
   * @param ndoc destination
   */
  private static void copyHeader(
      final Document doc,
      final Document ndoc) {
    NodeList heads = doc.getElementsByTagName("head");

    if (heads.getLength() > 0) {
      Element head = (Element) heads.item(0);
      ndoc.getDocumentElement().insertBefore(
          ndoc.adoptNode(head.cloneNode(true)),
          ndoc.getDocumentElement().getFirstChild()
      );
    }
  }

  /**
   * A part of a document between two page-breaks
   *
   * @author Quentin Anciaux
   * @version 0.94
   */
  private record PbDocument(
      Element pageBreakStart,
      Element pageBreakEnd
  ) {}
}
