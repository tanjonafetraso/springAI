package com.example.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.servlet.http.HttpSession;

@RestController
public class controller {
	private final String key = "demo";

//	interface Assistant{
//		@SystemMessage("tu dois être intelligent")
//		String chat(@UserMessage String userMessage);
//	}
//	
//	@GetMapping("/chat")
//	public String chatTeste(@RequestParam String userMessage) {
//		OpenAiChatModel model=OpenAiChatModel.withApiKey(key);
//		Assistant assistant=AiServices.create(Assistant.class, model);
//		String response=assistant.chat(userMessage);
//		return response;
//	}
//	
//	
//	
//	
//	
//	 // Simulation d'une gestion de session simple avec un cache
//    private Map<String, Assistant> sessionAssistants = new HashMap<>();
//
//    @GetMapping("/path")
//    public String remember(@RequestParam String message, HttpSession session) {
//        String sessionId = session.getId();
//
//        // Récupérer l'assistant de la session ou créer un nouveau si nécessaire
//        Assistant assistant = sessionAssistants.get(sessionId);
//        if (assistant == null) {
//            assistant = AiServices.builder(Assistant.class)
//                .chatLanguageModel(OpenAiChatModel.withApiKey(key))
//                .chatMemory(MessageWindowChatMemory.withMaxMessages(1000))
//                .build();
//            sessionAssistants.put(sessionId, assistant);
//        }
//
//        // Demander une réponse à l'assistant
//        String response = assistant.chat(message);
//        return response;
//    }

	interface Assistant {
		String chat(@UserMessage String userMessage);
	}

	@GetMapping("/teste")
	public String chat(@RequestParam("m") String m) {
		String apiKey = "demo";

		// Initialize the OpenAI model
//		OpenAiChatModel model = OpenAiChatModel.withApiKey(apiKey);
		ChatLanguageModel model = OpenAiChatModel.builder().apiKey(apiKey).modelName("gpt-3.5-turbo").temperature(0.3)

				.logRequests(true).logResponses(true).build();
		// Create the AI service
		Assistant assistant = AiServices.create(Assistant.class, model);

		// Use the AI service to handle a user message
		String response = assistant.chat(m);
		System.out.println(response);
		return response;
	}

	@GetMapping("/embedding")
	public void embedding() {
		EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder().host("localhost").port(5433)
				.database("SpringAI1").user("postgres").password("admin").table("test").dimension(384).build();

		EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

		TextSegment segment1 = TextSegment.from("mon est tanjona ");
		Embedding embedding1 = embeddingModel.embed(segment1).content();
		embeddingStore.add(embedding1, segment1);

		TextSegment segment2 = TextSegment.from("j'habite à avaratra ankatso");
		Embedding embedding2 = embeddingModel.embed(segment2).content();
		embeddingStore.add(embedding2, segment2);

		Embedding queryEmbedding = embeddingModel.embed("où j'habite?").content();
		List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 1);
		EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);

		System.out.println(embeddingMatch.score()); // 0.8144288608390052
		System.out.println(embeddingMatch.embedded().text()); // I like football.
	}

}
