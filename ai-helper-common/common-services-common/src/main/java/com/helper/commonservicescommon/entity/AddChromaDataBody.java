package com.helper.commonservicescommon.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddChromaDataBody {
    private List<String> documents;
    private float[][] embeddings;
    private List<String> ids;
    private List<String> uris;
    private List<ChromaDataMetaData> metadatas;
}
