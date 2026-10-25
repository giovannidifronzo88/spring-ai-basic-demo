package com.test.spring.ai.springaibasicdemo.service;

import com.test.spring.ai.springaibasicdemo.model.CollectionType;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class VectorStoreResolver {

    private final Map<CollectionType, VectorStore> stores;

    public VectorStoreResolver(
            @Qualifier("ccnlVectorStore") VectorStore ccnlVectorStore,
            @Qualifier("baseVectorStore") VectorStore baseVectorStore) {

        this.stores = new EnumMap<>(CollectionType.class);
        this.stores.put(CollectionType.CCNL, ccnlVectorStore);
        this.stores.put(CollectionType.BASE, baseVectorStore);
    }

    public VectorStore resolve(CollectionType collectionType) {
        VectorStore store = stores.get(collectionType);

        if (store == null) {
            throw new IllegalArgumentException("Collection non supportata: " + collectionType);
        }

        return store;
    }
}
