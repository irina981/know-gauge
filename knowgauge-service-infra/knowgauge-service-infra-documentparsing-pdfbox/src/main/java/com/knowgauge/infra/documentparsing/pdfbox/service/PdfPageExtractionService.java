package com.knowgauge.infra.documentparsing.pdfbox.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import com.knowgauge.core.port.documentparser.DocumentParser;

@Service
public class PdfPageExtractionService implements DocumentParser {
	private static final String CONTENT_TYPE = "application/pdf";

	@Override
	public List<String> extractPages(InputStream pdfStream) throws Exception {
		try (RandomAccessRead rar = new RandomAccessReadBuffer(pdfStream); PDDocument document = Loader.loadPDF(rar)) {
			PDFTextStripper stripper = new PDFTextStripper();
			List<String> pages = new ArrayList<>();

			int pageCount = document.getNumberOfPages();
			for (int page = 1; page <= pageCount; page++) {
				stripper.setStartPage(page);
				stripper.setEndPage(page);

				String text = stripper.getText(document);
				pages.add(normalize(text));
			}
			return pages;
		}
	}

	private static String normalize(String text) {
		return text.replace("\r\n", "\n").replace("\r", "\n").replaceAll("[ \t]+", " ").trim();
	}

	@Override
	public String contentType() {
		return CONTENT_TYPE;
	}

}
