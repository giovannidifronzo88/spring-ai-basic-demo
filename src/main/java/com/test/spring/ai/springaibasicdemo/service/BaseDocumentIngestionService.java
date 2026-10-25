package com.test.spring.ai.springaibasicdemo.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;



@Service
public class BaseDocumentIngestionService  {

    @Qualifier("baseVectorStore")
    private final VectorStore vectorStore;

    public BaseDocumentIngestionService(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
    }


    public void ingest(InputStream inputStream) throws IOException {

        List<Document> documents = new ArrayList<>();

        try (
                Reader reader = new InputStreamReader(
                        inputStream, StandardCharsets.UTF_8
                );
                CSVParser parser = CSVFormat.DEFAULT.builder()
                            .setHeader()
                            .setSkipHeaderRecord(true)
                            .get()
                            .parse(reader)
        ) {
            for (CSVRecord record : parser) {
                String source = record.get("source");
                String content = record.get("content");

                if (content != null && !content.isBlank()) {
                    documents.add(new Document(
                                content,
                                Map.of("source", source)
                        ));
                    }
                }
            }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
    }


}

