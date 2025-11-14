package com.helper.services.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChromaSearchResponse {
    public List<List<String>> ids;
    public List<List<String>> documents;
    public List<List<Map<String, Object>>> metadatas;
    public List<List<Double>> distances;
}