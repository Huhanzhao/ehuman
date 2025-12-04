package com.helper.commonservicescommon.entity;

import lombok.Data;

import java.util.List;

@Data
public class AddChromaDataBody {
    private List<String> documents;
    private float[][] embeddings;
    private List<String> ids;
    private List<String> uris;
    private List<ChromaDataMetaData> metadatas;
}
