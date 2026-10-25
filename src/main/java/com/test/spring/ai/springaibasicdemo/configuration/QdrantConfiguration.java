package com.test.spring.ai.springaibasicdemo.configuration;

import io.grpc.ManagedChannelBuilder;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class QdrantConfiguration {

    @Bean
    QdrantClient qdrantClient(
            @Value("${spring.ai.vectorstore.qdrant.host}") String host,
            @Value("${spring.ai.vectorstore.qdrant.port}") int port,
            @Value("${spring.ai.vectorstore.qdrant.api-key:}") String apiKey,
            @Value("${spring.ai.vectorstore.qdrant.use-tls:false}") boolean useTls) {

        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);

        if (useTls) {
            channelBuilder.useTransportSecurity();
        } else {
            channelBuilder.usePlaintext();
        }

        var channel = channelBuilder.build();


        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(channel);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.withApiKey(apiKey);
        }
        return new QdrantClient(builder.build());
    }

    @Bean
    @Qualifier("baseVectorStore")
    VectorStore baseVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel,
                                @Value("${spring.ai.vectorstore.qdrant.collection-name}") String collectionName,
                                @Value("${spring.ai.vectorstore.qdrant.initialize-schema}") boolean initializeSchema) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(collectionName)
                .initializeSchema(initializeSchema)
                .build();
    }

    @Bean
    @Qualifier("ccnlVectorStore")
    VectorStore ccnlVectorStore(
            QdrantClient qdrantClient,
            EmbeddingModel embeddingModel,
            @Value("${app.qdrant.collections.ccnl.name}") String collectionName,
            @Value("${app.qdrant.collections.ccnl.initialize-schema}") boolean initializeSchema) {

        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(collectionName)
                .initializeSchema(initializeSchema)
                .build();
    }
}
