package com.knowgauge.infra.testgeneration.openai.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.port.testgeneration.LlmTestGenerationService;
import com.knowgauge.infra.testgeneration.openai.mapper.ChatResponseMapper;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

@Service
public class LlmTestGenerationServiceImpl implements LlmTestGenerationService {

    private final ChatModel chatModel;
    private final ChatResponseMapper responseMapper;

    @Value("${kg.testgen.testgen.strict-json:true}")
    private boolean strictJson;

    public LlmTestGenerationServiceImpl( @Qualifier("testGenerationChatModel")ChatModel chatModel, ChatResponseMapper responseMapper) {
        this.chatModel = chatModel;
		this.responseMapper = responseMapper;
    }

    @Override
    public List<TestQuestion> generate(String prompt, Test test) {
        if (prompt == null || prompt.isBlank()) {
            return Collections.emptyList();
        }

        String effectivePrompt = strictJson ? enforceJsonOnly(prompt) : prompt;

        ChatResponse response = chatModel.chat(UserMessage.from(effectivePrompt));

        return responseMapper.map(response, test);
    }


    private String enforceJsonOnly(String basePrompt) {
        return basePrompt + "\n\nReturn ONLY a valid JSON array. No markdown. No explanations.";
    }
}
