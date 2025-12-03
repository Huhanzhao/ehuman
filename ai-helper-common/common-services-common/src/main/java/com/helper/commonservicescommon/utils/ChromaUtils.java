package com.helper.commonservicescommon.utils;


import com.helper.commonservicescommon.entity.AddChromaDataBody;
import com.helper.commonservicescommon.entity.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ChromaUtils {

    private final WebClient chromaWebClient;

    public ChromaUtils(WebClient.Builder webClientBuilder) {
        // 指定 baseUrl
        this.chromaWebClient = webClientBuilder.baseUrl("http://localhost:8000").build();
    }

    // 查询是否存在租户
    public boolean isTenantExists(String tenantId) {
        try {
            chromaWebClient.get()
                    .uri("/api/v2/tenants/" + tenantId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }catch(Exception e){
            log.info("租户不存在{}", tenantId);
            return false;
        }
        return true;
    }

    // 创建租户
    public void createTenant(String tenantId) {
        try {
            chromaWebClient.post()
                    .uri("/api/v2/tenants")
                    .bodyValue(Map.of("name", tenantId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }catch (Exception e){
            throw new BusinessException("创建租户失败");
        }
        log.info("租户创建成功{}", tenantId);
    }

    // 给租户创建数据库
    public void createDatabase(String tenantId, String databaseName) {
        try {
            chromaWebClient.post()
                    .uri("/api/v2/tenants/" + tenantId + "/databases")
                    .bodyValue(Map.of("name", databaseName))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }catch (Exception e){
            throw new BusinessException("创建数据库失败");
        }
        log.info("数据库创建成功{}", databaseName);
    }

    // 在数据库下面创建集合
    public void createCollection(String tenantId, String databaseName,String collectionName,Map<String,String> metadata) {

        Map<String,Object> bodyMap = new HashMap<>();
        bodyMap.put("name", collectionName);
        bodyMap.put("metadata", metadata);
        bodyMap.put("schema", null);
        bodyMap.put("get_or_create", true);
        bodyMap.put("configuration", null);

        try {
            chromaWebClient.post()
                    .uri("/api/v2/tenants/" +
                            tenantId +
                            "/databases/" +
                            databaseName +
                            "/collections")
                    .bodyValue(bodyMap)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }catch (Exception e){
            throw new BusinessException("集合创建失败");
        }
        log.info("集合创建成功{}", databaseName);
    }

    // 用名称查询集合的uuid  /api/v2/tenants/{tenant}/databases/{database}/collections/{collection_id}
    public String getCollectionUuid(String tenantId, String databaseName, String collectionName) {
        try {
            String block = chromaWebClient.get()
                    .uri("/api/v2/tenants/" +
                            tenantId +
                            "/databases/" +
                            databaseName +
                            "/collections/" +
                            collectionName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("集合查询成功：{}", collectionName);
            return block.substring(block.indexOf("\"id\":\"") + 6, block.indexOf("\",\"name\":\""));
        }catch (Exception e){
            throw new BusinessException("集合不存在请先创建" + collectionName);
        }
    }

    // 将记录添加到集合中 /api/v2/tenants/{tenant}/databases/{database}/collections/{collection_id}/add
    public boolean addChromaData(String tenantId, String databaseName, String collectionName,
                                 AddChromaDataBody addChromaDataBody) {

        String collectionUuid = this.getCollectionUuid(tenantId, databaseName, collectionName);

        try {
            chromaWebClient.post()
                    .uri("/api/v2/tenants/" +
                            tenantId +
                            "/databases/" +
                            databaseName +
                            "/collections/" +
                            collectionUuid +
                            "/add")
                    .bodyValue(addChromaDataBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }catch (Exception e){
            log.info("chroma添加数据失败{}", addChromaDataBody);
            return false;
        }
        log.info("chroma添加数据成功{}", addChromaDataBody);
        return true;
    }

    // todo 根据userId和矢量 查询集合中的数据
    public String queryData(String tenantId, String databaseName, String collectionName, String userId, List<Float> vector){

        String collectionUuid = this.getCollectionUuid(tenantId, databaseName, collectionName);
        Map<String,Object> bodyMap = new HashMap<>();
        bodyMap.put("n_results",5); // 返回四条结果
        bodyMap.put("query_embeddings",new float[][]{new float[]{0.1f}});
        bodyMap.put("where",Map.of("userId",Map.of("$eq", userId)));  // 过滤元数据中的用户id

        String block =  chromaWebClient.post()
                .uri("/api/v2/tenants/" +
                        tenantId +
                        "/databases/" +
                        databaseName +
                        "/collections/" +
                        collectionUuid +
                        "/query")  // 集合的uuid 集合名查询chroma得到 可以用来区分用户
                .bodyValue(bodyMap)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return null;
    }

    // todo 删除集合中的数据，可以用元数据中的元素筛选

    // todo 删除集合

}
