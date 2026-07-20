package com.company.vectortool.interfaces;

import com.company.vectortool.models.VectorEmbeddings;
import java.util.UUID;

public interface IVectorDbAdaptor {
    void storeEmbedding(VectorEmbeddings embedding);
    void purgeEmbedding(UUID documentId);
}
