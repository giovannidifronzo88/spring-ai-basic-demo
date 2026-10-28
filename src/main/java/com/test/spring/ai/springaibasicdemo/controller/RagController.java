package com.test.spring.ai.springaibasicdemo.controller;

import com.test.spring.ai.springaibasicdemo.model.CollectionType;
import com.test.spring.ai.springaibasicdemo.service.BaseDocumentIngestionService;
import com.test.spring.ai.springaibasicdemo.service.CcnlDocumentIngestionService;
import com.test.spring.ai.springaibasicdemo.service.VectorStoreResolver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final ChatClient chatClient;
    private final VectorStoreResolver vectorStoreResolver;
    private final BaseDocumentIngestionService baseDocumentIngestionService;
    private final CcnlDocumentIngestionService ccnlDocumentIngestionService;
    private final ChatMemory chatMemory;

    public RagController(ChatClient chatClient, VectorStoreResolver vectorStoreResolver, BaseDocumentIngestionService baseDocumentIngestionService, CcnlDocumentIngestionService ccnlDocumentIngestionService, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.vectorStoreResolver = vectorStoreResolver;
        this.baseDocumentIngestionService = baseDocumentIngestionService;
        this.ccnlDocumentIngestionService = ccnlDocumentIngestionService;
        this.chatMemory = chatMemory;
    }

    @GetMapping("/qdrant")
    public String qdrantConnectionStatus() {
        try {
            // Proviamo a salvare un documento fittizio temporaneo
            Document docTest = new Document(java.util.UUID.randomUUID().toString(), "Test di connessione Spring AI", java.util.Map.of());
            vectorStoreResolver.resolve(CollectionType.BASE).add(List.of(docTest));

            return "Connessione con Qdrant Cloud riuscita! Raccolta creata/aggiornata correttamente.";
        } catch (Exception e) {
            return "Errore di connessione con Qdrant Cloud. Dettaglio: " + e.getMessage();
        }
    }

    @PostMapping(
            value = "/ingest/base",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> baseIngest(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        baseDocumentIngestionService.ingest(file.getInputStream());

        return ResponseEntity.ok("Ingestion completata");
    }

    @PostMapping("/ingest/ccnl")
    public ResponseEntity<String> ingestCcnl() throws IOException {
        ccnlDocumentIngestionService.ingest();
        return ResponseEntity.ok("Ingestion CCNL completata");
    }

    @GetMapping("/answer")
    public String prompt(
            @RequestParam("collection") String collection,
            @RequestParam("prompt") String prompt,
            @RequestHeader(value = "chatId", required = false) String chatId) {


        VectorStore store = vectorStoreResolver.resolve(CollectionType.valueOf(collection));
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(store)
                .searchRequest(SearchRequest.builder()
                        .topK(5)
                        .similarityThreshold(0.70)
                        .build())
                .build();


        return chatClient.prompt()
                .advisors(questionAnswerAdvisor)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .user(prompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }
}

