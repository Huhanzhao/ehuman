package com.helper.services.tools;


import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SearchWebTool {

    private final TavilyWebSearchEngine tavilyWebSearchEngine;

    public SearchWebTool(@Value("${tavily.api-key}") String apiKey){
        this.tavilyWebSearchEngine = TavilyWebSearchEngine.withApiKey(apiKey);
    }

    @Tool("搜索web")
    public WebSearchResults search(String query){
        log.info("搜索web方法被调用了,开始搜索：{}", query);
        return tavilyWebSearchEngine.search(query);
    }
}
