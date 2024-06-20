package com.example.demo;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.controller.Assistant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

@RestController
public class EmbeddingController {
//	interface Assistant{
//	@SystemMessage("tu dois me repondre à partir du document dans le contexte si tu ne troue pas dis que tu ne sais rien à Tanjona")
//	String chat(@UserMessage String userMessage);
//}
	private final EmbeddingStore<TextSegment> embeddingStore;
	private final EmbeddingModel embeddingModel;

	@Value("classpath:documents/tanjona.pdf")
	Resource resourcePdf;

	public EmbeddingController() {
		this.embeddingStore = PgVectorEmbeddingStore.builder().host("localhost").port(5433).database("SpringAI1")
				.user("postgres").password("admin").table("test").dimension(1000).build();
		this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
	}

	@GetMapping("/embeddings")
	public void embedding() {
		try {
			// Lire le texte d'un fichier PDF
//           File pdfFile = new File("path/to/your/document.pdf"); // Remplacez par le chemin réel du fichier PDF
			PDDocument document = PDDocument.load(resourcePdf.getInputStream());

			PDFTextStripper pdfStripper = new PDFTextStripper();
			String text = pdfStripper.getText(document);
			document.close();
			
			// Diviser le texte en paragraphes
			List<String> tokenizedSegments = tokenizeText(text, 1000);
			for (int i = 0; i < tokenizedSegments.size(); i++) {
				// Ajouter le texte extrait à l'EmbeddingStore
				TextSegment segment = TextSegment.from(tokenizedSegments.get(i));
				Embedding embedding = embeddingModel.embed(segment).content();
				embeddingStore.add(embedding, segment);
			}

           // Exemple de requête d'embedding
           Embedding queryEmbedding = embeddingModel.embed("What is the document about?").content();
           List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 1);
           EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);

           System.out.println(embeddingMatch.score());
           System.out.println(embeddingMatch.embedded().text());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	@GetMapping("/chat")
	public String chatTeste(@RequestParam String userMessage) {
		// Générer un embedding pour le message utilisateur
		Embedding queryEmbedding = embeddingModel.embed(userMessage).content();
		// Trouver les segments de texte pertinents
		List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 1);
		String context = "";
		if (!relevant.isEmpty()) {
			EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);
			context = embeddingMatch.embedded().text();
		}
System.out.println(context);
		// Utiliser OpenAI pour répondre à la question avec le contexte
		OpenAiChatModel model = OpenAiChatModel.withApiKey("demo");
		Assistant assistant = AiServices.create(Assistant.class, model);

		// Inclure le contexte trouvé dans la question posée à l'assistant
		String response = assistant.chat(userMessage + " Contexte: "
				+ context);

		return response ;
	}

	private List<String> tokenizeText(String text, int maxTokens) {
		List<String> segments = new ArrayList<>();
		StringTokenizer tokenizer = new StringTokenizer(text);
		StringBuilder segmentBuilder = new StringBuilder();
		int tokenCount = 0;

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (tokenCount + token.length() > maxTokens) {
				// Ajouter le segment actuel à la liste et recommencer avec un nouveau segment
				segments.add(segmentBuilder.toString());
				segmentBuilder = new StringBuilder();
				tokenCount = 0;
			}
			segmentBuilder.append(token).append(" ");
			tokenCount += token.length() + 1; // +1 pour l'espace
		}

		// Ajouter le dernier segment restant
		if (segmentBuilder.length() > 0) {
			segments.add(segmentBuilder.toString());
		}

		return segments;
	}
}
