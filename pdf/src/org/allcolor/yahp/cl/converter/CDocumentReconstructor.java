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

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import com.google.errorprone.annotations.CheckReturnValue;
import com.lowagie.text.Meta;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer.CConvertException;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer.CHeaderFooter;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer.PageSize;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use iText to construct a complete pdf document from differents pdf parts. Apply configured
 * security policies on the resulting pdf.
 *
 * @author Quentin Anciaux
 * @version 1.2.20b
 */
@NullMarked
@CheckReturnValue
public class CDocumentReconstructor {

  private static final Logger log = LoggerFactory.getLogger(CDocumentReconstructor.class);
  private static final Pattern RE_PAGE_NUMBER = Pattern.compile("<pagenumber>");
  private static final Pattern RE_PAGE_COUNT = Pattern.compile("<pagecount>");

  /**
   * return the itext security flags for encryption
   *
   * @param properties the converter properties
   * @return the itext security flags
   */
  private static int getSecurityFlags(final Map<String, String> properties) {
    int securityType = 0;
    if ("true".equals(properties.get(IHtmlToPdfTransformer.PDF_ALLOW_PRINTING))) {
      securityType |= PdfWriter.ALLOW_PRINTING;
    }
    if ("true".equals(properties.get(IHtmlToPdfTransformer.PDF_ALLOW_MODIFY_CONTENTS))) {
      securityType |= PdfWriter.ALLOW_MODIFY_CONTENTS;
    }
    if ("true".equals(properties.get(IHtmlToPdfTransformer.PDF_ALLOW_COPY))) {
      securityType |= PdfWriter.ALLOW_COPY;
    }
    if ("true".equals(properties.get(IHtmlToPdfTransformer.PDF_ALLOW_MODIFT_ANNOTATIONS))) {
      securityType |= PdfWriter.ALLOW_MODIFY_ANNOTATIONS;
    }
    if ("true".equals(properties.get(IHtmlToPdfTransformer.PDF_ALLOW_FILLIN))) {
      securityType |= PdfWriter.ALLOW_FILL_IN;
    }
    if ("true".equals(properties.get(IHtmlToPdfTransformer.PDF_ALLOW_SCREEN_READERS))) {
      securityType |= PdfWriter.ALLOW_SCREENREADERS;
    }
    if ("true".equals(properties.get(IHtmlToPdfTransformer.PDF_ALLOW_ASSEMBLY))) {
      securityType |= PdfWriter.ALLOW_ASSEMBLY;
    }
    if ("true".equals(properties.get(IHtmlToPdfTransformer.PDF_ALLOW_DEGRADED_PRINTING))) {
      securityType |= PdfWriter.ALLOW_DEGRADED_PRINTING;
    }

    return securityType;
  }

  /**
   * construct a pdf document from pdf parts.
   *
   * @param files      list containing the pdf to assemble
   * @param properties converter properties
   * @param fout       outputstream to write the new pdf
   * @param baseUrl    base url of the document
   * @param producer   producer of the pdf
   * @throws CConvertException if an error occurred while reconstruct.
   */
  public static void reconstruct(final List<File> files, final Map<String, String> properties,
      final OutputStream fout, @Nullable final String baseUrl,
      final String producer, final List<PageSize> size, final List<CHeaderFooter> headersFooters)
      throws CConvertException {
    OutputStream out = fout;
    OutputStream out2;
    boolean signed = false;
    OutputStream oldOut;
    File tmp = null;
    File tmp2 = null;
    try {
      tmp = File.createTempFile("yahp", "pdf");
      tmp2 = File.createTempFile("yahp", "pdf");
      oldOut = out;
      if ("true".equals(properties.get(IHtmlToPdfTransformer.USE_PDF_SIGNING))) {
        signed = true;
        out2 = new FileOutputStream(tmp2);
      } else {
        out2 = oldOut;
      }
      out = new FileOutputStream(tmp);
      com.lowagie.text.Document document = null;
      PdfCopy writer = null;
      boolean first = true;

      Map<String, String> mapSizeDoc = new HashMap<>();

      int totalPage = 0;

      for (int i = 0; i < files.size(); i++) {
        final File fPDF = files.get(i);
        final PdfReader reader = new PdfReader(fPDF.getAbsolutePath());
        reader.consolidateNamedDestinations();

        final int n = reader.getNumberOfPages();

        if (first) {
          first = false;
          document = new com.lowagie.text.Document(reader.getPageSizeWithRotation(1));
          writer = new PdfCopy(document, out);
          writer.setPdfVersion(PdfWriter.VERSION_1_3);
          writer.setFullCompression();

          // check if encryption is needed
          if ("true".equals(properties.get(IHtmlToPdfTransformer.USE_PDF_ENCRYPTION))) {
            final String password = properties.get(IHtmlToPdfTransformer.PDF_ENCRYPTION_PASSWORD);
            final int securityType = CDocumentReconstructor.getSecurityFlags(properties);
            writer.setEncryption(password.getBytes(UTF_8), null, securityType,
                PdfWriter.STANDARD_ENCRYPTION_128);
          }

          final String title = properties.get(IHtmlToPdfTransformer.PDF_TITLE);

          if (title != null) {
            document.addTitle(title);
          } else if (baseUrl != null) {
            document.addTitle(baseUrl);
          }

          document.addCreator(requireNonNullElse(
              properties.get(IHtmlToPdfTransformer.PDF_CREATOR), IHtmlToPdfTransformer.VERSION));

          final String author = properties.get(IHtmlToPdfTransformer.PDF_AUTHOR);

          if (author != null) {
            document.addAuthor(author);
          }

          document.add(new Meta("Producer", producerMetaValue(properties, producer)));
          document.open();
        }

        PdfImportedPage page;

        for (int j = 0; j < n; ) {
          ++j;
          totalPage++;
          mapSizeDoc.put("" + totalPage, "" + i);
          page = writer.getImportedPage(reader, j);
          writer.addPage(page);
        }
      }

      document.close();
      out.flush();
      out.close();
      {
        final PdfReader reader = new PdfReader(tmp.getAbsolutePath());

        final int n = reader.getNumberOfPages();
        final PdfStamper stp = new PdfStamper(reader, out2);
        int i = 0;
        BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        final CHtmlToPdfFlyingSaucerTransformer trans = new CHtmlToPdfFlyingSaucerTransformer();
        while (i < n) {
          i++;
          int indexSize = parseInt(mapSizeDoc.get("" + i));
          final int[] dsize = size.get(indexSize).getSize();
          final int[] dmargin = size.get(indexSize).getMargin();
          for (CHeaderFooter chf : headersFooters) {
            if (chf.getSfor().equals(CHeaderFooter.ODD_PAGES) && i % 2 == 0) {
              continue;
            } else if (chf.getSfor().equals(CHeaderFooter.EVEN_PAGES) && i % 2 != 0) {
              continue;
            }
            final String text = replacePageCount(replacePageNumber(chf.getContent(), i), n);

            // text over the existing page
            final PdfContentByte over = stp.getOverContent(i);
            final ByteArrayOutputStream bbout = new ByteArrayOutputStream();
            if (chf.getType().equals(CHeaderFooter.HEADER)) {
              trans.transform(new ByteArrayInputStream(text.getBytes(UTF_8)), baseUrl,
                  new PageSize(dsize[0] - (dmargin[0] + dmargin[1]), dmargin[3]), new ArrayList(),
                  properties, bbout);
            } else if (chf.getType().equals(CHeaderFooter.FOOTER)) {
              trans.transform(new ByteArrayInputStream(text.getBytes(UTF_8)), baseUrl,
                  new PageSize(dsize[0] - (dmargin[0] + dmargin[1]), dmargin[2]), new ArrayList(),
                  properties, bbout);
            }
            final PdfReader readerHF = new PdfReader(bbout.toByteArray());
            if (chf.getType().equals(CHeaderFooter.HEADER)) {
              over.addTemplate(stp.getImportedPage(readerHF, 1), dmargin[0], dsize[1] - dmargin[3]);
            } else if (chf.getType().equals(CHeaderFooter.FOOTER)) {
              over.addTemplate(stp.getImportedPage(readerHF, 1), dmargin[0], 0);
            }
            readerHF.close();
          }
        }
        stp.close();
      }
      flushAndClose(out2);
      if (signed) {

        final String keyPassword = properties.get(
            IHtmlToPdfTransformer.PDF_SIGNING_PRIVATE_KEY_PASSWORD);
        final String password = properties.get(IHtmlToPdfTransformer.PDF_ENCRYPTION_PASSWORD);
        final String keyStorePassword = properties.get(
            IHtmlToPdfTransformer.PDF_SIGNING_KEYSTORE_PASSWORD);
        final String privateKeyFile = properties.get(
            IHtmlToPdfTransformer.PDF_SIGNING_PRIVATE_KEY_FILE);
        final String reason = properties.get(IHtmlToPdfTransformer.PDF_SIGNING_REASON);
        final String location = properties.get(IHtmlToPdfTransformer.PDF_SIGNING_LOCATION);
        final boolean selfSigned = !"false".equals(properties.get(IHtmlToPdfTransformer.USE_PDF_SELF_SIGNING));
        final PdfReader reader = readPdf(tmp2, password);

        final KeyStore ks = keyStore(privateKeyFile, keyStorePassword, selfSigned);

        final String alias = ks.aliases().nextElement();
        final PrivateKey key = (PrivateKey) ks.getKey(alias, keyPassword.toCharArray());
        final Certificate[] chain = ks.getCertificateChain(alias);
        final PdfStamper stamper = PdfStamper.createSignature(reader, oldOut, '\0');

        if ("true".equals(properties.get(IHtmlToPdfTransformer.USE_PDF_ENCRYPTION))) {
          stamper.setEncryption(PdfWriter.STANDARD_ENCRYPTION_128, password, null, getSecurityFlags(properties));
        }

        final PdfSignatureAppearance sap = stamper.getSignatureAppearance();

        sap.setCrypto(key, chain, null,
            selfSigned ? PdfSignatureAppearance.SELF_SIGNED : PdfSignatureAppearance.WINCER_SIGNED);

        if (reason != null) {
          sap.setReason(reason);
        }

        if (location != null) {
          sap.setLocation(location);
        }

        stamper.close();
        oldOut.flush();
      }
    } catch (final Exception e) {
      throw new CConvertException("Failed to reconstruct the pdf document: " + e.getMessage(), e);
    } finally {
      deleteFile(tmp);
      deleteFile(tmp2);
    }
  }

  private static String replacePageNumber(String html, int pageNumber) {
    return RE_PAGE_NUMBER.matcher(html).replaceAll(String.valueOf(pageNumber));
  }

  private static String replacePageCount(String html, int pageCount) {
    return RE_PAGE_COUNT.matcher(html).replaceAll(String.valueOf(pageCount));
  }

  private static String producerMetaValue(Map<String, String> properties, String producer) {
    return requireNonNullElseGet(
        properties.get(IHtmlToPdfTransformer.PDF_PRODUCER),
        () -> "%s - http://www.allcolor.org/YaHPConverter/ - %s".formatted(
            IHtmlToPdfTransformer.VERSION, producer)
    );
  }

  private static PdfReader readPdf(File source, String password) throws IOException {
    return password != null ?
        new PdfReader(source.getAbsolutePath(), password.getBytes(UTF_8)) :
        new PdfReader(source.getAbsolutePath());
  }

  private static KeyStore keyStore(String privateKeyFile, String keyStorePassword,
      boolean selfSigned)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    KeyStore keyStore = selfSigned ?
        KeyStore.getInstance(KeyStore.getDefaultType()) :
        KeyStore.getInstance("pkcs12");

    keyStore.load(new FileInputStream(privateKeyFile), keyStorePassword.toCharArray());
    return keyStore;
  }

  private static void deleteFile(File tmp) {
    try {
      boolean deleted = tmp.delete();
      if (!deleted) {
        log.warn("Failed to delete temporary file: {}", tmp.getAbsolutePath());
      }
    } catch (Exception e) {
      log.warn("Failed to delete temporary file: {}", tmp.getAbsolutePath(), e);
    }
  }

  private static void flushAndClose(OutputStream out) {
    try (out) {
      try {
        out.flush();
      } catch (IOException e) {
        log.warn("Failed to flush stream", e);
      }
    } catch (IOException e) {
      log.warn("Failed to close stream", e);
    }
  }
}