package com.test.spring.ai.springaibasicdemo.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class CcnlDocumentIngestionService implements DocumentIngestionService {

    private final VectorStore vectorStore;
    private final String defaultPdfResource;

    public CcnlDocumentIngestionService(@Qualifier("ccnlVectorStore") VectorStore vectorStore,
                                        @Value("${app.ingestion.pdf-resource}") String defaultPdfResource) {
        this.vectorStore = vectorStore;
        this.defaultPdfResource = defaultPdfResource;
    }


    @Override
    public void ingest() throws IOException {
        ingestPdfFromClasspath(defaultPdfResource);
    }

    public void ingestPdfFromClasspath(String classpathLocation) throws IOException {

        Resource resource = new ClassPathResource(classpathLocation);

        if (!resource.exists()) {
            throw new IllegalArgumentException("PDF non trovato nel classpath: " + classpathLocation);
        }
        String text;

        try (PDDocument pdf = Loader.loadPDF(resource.getInputStream().readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(pdf);
        }

        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Il PDF non contiene testo estraibile.");
        }

        TokenTextSplitter splitter = TokenTextSplitter.builder().build();
        List<Document> pageDocuments = List.of(new Document(text, Map.of("source", classpathLocation)));
        List<Document> chunks = splitter.apply(pageDocuments);

        vectorStore.add(chunks);
    }
}




