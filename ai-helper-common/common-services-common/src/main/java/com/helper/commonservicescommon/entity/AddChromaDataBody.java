package com.helper.commonservicescommon.entity;

import lombok.Data;

import java.util.List;

@Data
public class AddChromaDataBody {
    private List<String> documents;
    private List<List<Double>> embeddings;
    private List<String> ids;
    private List<String> uris;
    private List<ChromaDataMetaData> metadatas;
}
