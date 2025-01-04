package jcfgonc.mapper.chatbots;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;

import graph.GraphReadWrite;
import graph.StringEdge;
import graph.StringGraph;
import jcfgonc.mapper.chatbots.json.Candidate;
import jcfgonc.mapper.chatbots.json.ChatBotReply;
import jcfgonc.mapper.chatbots.json.Content;
import jcfgonc.mapper.chatbots.json.Part;
import structures.Ticker;

public class AnalogyToText {

	private static final Gson GSON = new Gson();

	public static void main(String[] args) throws NoSuchFileException, IOException {
		// analogy format I get from my excel file
		String txt = "fridge|orchestra	fridge|orchestra,atlocation,south london|concert hall;fridge|orchestra,usedfor,refrigeration|make music;yogurt|instrument,atlocation,fridge|orchestra;fridge|orchestra,isa,refrigerator|group of musician;fridge|orchestra,partof,kitchen|theatre;fridge|orchestra,capableof,cool food|play symphony;oven|band,antonym,fridge|orchestra;";
		String[] tokens = txt.split("\t");

		String startingVertex = tokens[0];
		StringGraph analogy = GraphReadWrite.readCSVFromString(tokens[1]);
		// remove crappy relations
		analogy.removeEdgesByLabel("synonym");
		analogy.removeEdgesByLabel("antonym");

		// get central concepts of the analogy
		String[] concepts = startingVertex.split("\\|");
		String concept_a = concepts[0];
		String concept_b = concepts[1];

		HashMap<String, String> rt_templates = readRelationToTextTemplates("data/relation_to_text_templates.tsv", "\t", true);
		ArrayList<StringEdge> edgeSeq = createEdgeSequenceBFS(analogy, startingVertex);
		String analogy_facts = textualizeAnalogy(edgeSeq, rt_templates);
		String prompt_template = readFileToString("data/prompt_template_1.txt");
		String prompt = prompt_template.replace("%a%", concept_a).replace("%b%", concept_b).replace("%text%", analogy_facts);

		String reply = sendRequest(prompt);
		System.out.println(reply);

		System.lineSeparator();
	}

	private static String textualizeAnalogy(ArrayList<StringEdge> edgeSeq, HashMap<String, String> rt_templates) {
		String text = "";
		for (StringEdge edge : edgeSeq) {
			String e_source = edge.getSource();
			String e_target = edge.getTarget();
			String relation = edge.getLabel();
			String[] sources = e_source.split("\\|");
			String[] targets = e_target.split("\\|");
			String a = sources[0];
			String b = targets[0];
			String c = sources[1];
			String d = targets[1];
			String template = rt_templates.get(relation);
			text += template.replace("%a%", a).replace("%b%", b).replace("%c%", c).replace("%d%", d) + ". ";
			System.lineSeparator();
		}
		// TODO Auto-generated method stub
		return text;
	}

	private static HashMap<String, String> readRelationToTextTemplates(String filePath, String delimiter, boolean containsHeader) {
		HashMap<String, String> templates = new HashMap<String, String>();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

			if (containsHeader) {
				br.readLine();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(delimiter);
				assert tokens.length >= 2; // must have relation and template
				String relation = tokens[0];
				String template = tokens[1];
				templates.put(relation, template);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return templates;
	}

	/**
	 * Creates an edge sequence using a breadth first edge expansion starting at the given vertex. Used to generate textual descriptions of a graph.
	 * 
	 * @param graph
	 * @param startingVertex
	 * @return
	 */
	public static ArrayList<StringEdge> createEdgeSequenceBFS(StringGraph graph, String startingVertex) {
		ArrayList<StringEdge> edgeSequence = new ArrayList<StringEdge>();
		ArrayDeque<StringEdge> edgesToVisit = new ArrayDeque<StringEdge>();
		HashSet<StringEdge> visitedEdges = new HashSet<StringEdge>();
		edgesToVisit.addAll(graph.edgesOf(startingVertex));

		while (!edgesToVisit.isEmpty()) {
			StringEdge edge = edgesToVisit.remove();
			if (visitedEdges.contains(edge))
				continue;
			edgeSequence.add(edge);
			// System.out.println(edge);
			Set<StringEdge> neighboringEdges = graph.edgesOf(edge);
			for (StringEdge nextEdge : neighboringEdges) {
				if (visitedEdges.contains(nextEdge))
					continue;
				edgesToVisit.add(nextEdge);
			}
			visitedEdges.add(edge);
		}
		return edgeSequence;
	}

	public static String sendRequest(String text) {
		try {
			String api_call = readFileToString("data/google_gemini_api_query.txt");
			String apikey = readFileToString("data/apikey.txt");

			Ticker t = new Ticker();

			///// models as of 19/11/2024
			// gemini-1.5-pro
			// gemini-1.5-flash
			// gemini-1.5-flash-8b
			String model = "gemini-1.5-flash";
			String apiurl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apikey;

			URL url = new URI(apiurl).toURL();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
//			connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes()));
			String requestContents = api_call.replace("%text%", text);
			byte[] out = requestContents.getBytes(StandardCharsets.UTF_8);
			OutputStream stream = connection.getOutputStream();
			stream.write(out);
			stream.flush();

			System.out.println(connection.getResponseCode() + " " + connection.getResponseMessage()); // THis is optional

			InputStream inputStream = connection.getInputStream();
			String raw_reply = toString(inputStream);
			System.out.println(raw_reply);
			inputStream.close();
			connection.disconnect();
			t.showTimeDeltaLastCall();

			// process chatbot reply in json format
			ChatBotReply chatbotReply = GSON.fromJson(raw_reply, ChatBotReply.class);
			ArrayList<Candidate> candidates = chatbotReply.candidates;
			Candidate candidate = candidates.get(0);
			Content content = candidate.content;
			ArrayList<Part> parts = content.parts;
			Part part = parts.get(0);
			String chatbotText = part.text;

			return chatbotText;
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Failed successfully");
		}
		return null;
	}

	public static String toString(InputStream input) throws IOException {
		return new String(input.readAllBytes(), StandardCharsets.UTF_8);
	}

	public static String readFileToString(String filename) throws IOException {
		String text = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
		return text;
	}
}
