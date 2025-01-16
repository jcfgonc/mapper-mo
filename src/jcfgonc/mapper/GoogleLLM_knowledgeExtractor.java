package jcfgonc.mapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import com.google.gson.Gson;

import jcfgonc.mapper.chatbots.json.Candidate;
import jcfgonc.mapper.chatbots.json.ChatBotReply;
import jcfgonc.mapper.chatbots.json.Content;
import jcfgonc.mapper.chatbots.json.Part;
import structures.Ticker;
import utils.VariousUtils;

public class GoogleLLM_knowledgeExtractor {
	private static final Gson GSON = new Gson();

	public static void main(String[] args) {
		String concept = "vehicle";
		String prompt_template = "one fact per line, what are the most well known parts of a %concept% and their function? give their function with a single action verb";
		String prompt = prompt_template.replace("%concept%", concept);

		String reply = sendRequest(prompt);
		System.out.println(reply);
	}

	public static String sendRequest(String text) {
		try {
			String api_call = VariousUtils.readFile("data/google_gemini_api_query.txt");
			String apikey = VariousUtils.readFile("data/apikey.txt");

			//Ticker t = new Ticker();

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
			//System.out.println(raw_reply);
			inputStream.close();
			connection.disconnect();
			//t.showTimeDeltaLastCall();

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

}
