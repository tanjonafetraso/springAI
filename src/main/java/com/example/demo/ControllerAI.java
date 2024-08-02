package com.example.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.embedding.Embedding;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Splitter;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.servlet.http.HttpSession;
import com.google.common.collect.Lists;

@RestController
public class ControllerAI {

	private String key = "demo";
	private OpenAiChatModel model;
	private Map<String, Assistant> sessionAssistant;

	private EmbeddingStore<TextSegment> embeddingStore;
	private EmbeddingModel embeddingModel;
	@Value("classpath:document/these.pdf")
	private Resource resourcePDF;

	interface Assistant {
		@SystemMessage("tu dois être mon amie")
		String chat(String userMessage);
	}

	// constructeur

	/*
	 * 
	 * ICI c'est la script pour la creation de la table vector_store_teste dans la postegres
	 * 
	 * CREATE EXTENSION IF NOT EXISTS vector;	 * 
	 * CREATE TABLE IF NOT EXISTS public.vector_store_teste ( id uuid NOT NULL
	 * DEFAULT uuid_generate_v4(), content text COLLATE pg_catalog."default",
	 * metadata json, embedding vector(1536), CONSTRAINT vector_store_pkeys PRIMARY
	 * KEY (id) )
	 * 
	 */

	public ControllerAI() {
		embeddingStore = PgVectorEmbeddingStore.builder().host("localhost").port(5433).database("SpringAI1")
				.user("postgres").password("admin").table("vector_store").dimension(1000).build();
		embeddingModel = new AllMiniLmL6V2EmbeddingModel();
		model = OpenAiChatModel.builder().modelName("gpt-3.5-turbo").apiKey(key).build();
		sessionAssistant = new HashMap<>();
	}

	@GetMapping("/embedding/data")
	public String embeddingDataPdf() {
		try {
			// lire le document pdf
			PDDocument document = PDDocument.load(resourcePDF.getInputStream());
			PDFTextStripper textStripper = new PDFTextStripper();
			String text = textStripper.getText(document);
			document.close();

			// TextTokenizer le document en utilisent le chunks
			List<String> textTokenizers = Lists.newArrayList(Splitter.fixedLength(1000).split(text));

			// Vectoriser le document et enregistrer dans la DB vector
			for (String textTokenizer : textTokenizers) {
				TextSegment segment = TextSegment.from(textTokenizer);
				Embedding embedding = embeddingModel.embed(segment).content();
				embeddingStore.add(embedding, segment);
			}

			return "train ok";
		} catch (Exception e) {
			System.out.print(e.fillInStackTrace());
			return "train ko";
		}
	}

	@GetMapping("/chat/embedding/data/pdf")
	public String ChatDocumentPDF(@RequestParam String userMessage) {
		// recherche de similiarité dans la DBB
		Embedding queryEmbedding = embeddingModel.embed(userMessage).content();
		List<EmbeddingMatch<TextSegment>> retreiveDB = embeddingStore.findRelevant(queryEmbedding, 1);
		String context = "";
		if (!retreiveDB.isEmpty()) {
			EmbeddingMatch<TextSegment> embeddingMatch = retreiveDB.get(0);
			context = embeddingMatch.embedded().text();
		}
		// on va interroger le model LLMs à partir de la context
		Assistant assistant = AiServices.builder(Assistant.class).chatLanguageModel(model).build();
		String response = assistant.chat(userMessage + " Contexte " + context);
		return response;
	}

	@GetMapping("/chat")
	public String chatOpenAi(@RequestParam String userMessage, HttpSession session) {
		String sessionId = session.getId();
		Assistant assistant = sessionAssistant.get(sessionId);
		if (assistant == null) {
			assistant = AiServices.builder(Assistant.class).chatLanguageModel(model)
					.chatMemory(MessageWindowChatMemory.withMaxMessages(1000)).build();
			sessionAssistant.put(sessionId, assistant);
		}

		String response = assistant.chat(userMessage);
		return response;
	}

	@GetMapping("/chat/doc/pdf")
	public ModelAndView routerPageIndex2() {
		return new ModelAndView("index2.html");
	}

}
