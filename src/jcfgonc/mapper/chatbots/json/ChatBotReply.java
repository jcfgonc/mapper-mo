package jcfgonc.mapper.chatbots.json;

import java.util.ArrayList;

public class ChatBotReply {
	public ArrayList<Candidate> candidates;
	public UsageMetadata usageMetadata;
	public String modelVersion;

	@Override
	public String toString() {
		return "candidates=[" + candidates + "], usageMetadata=[" + usageMetadata + "], modelVersion=" + modelVersion;
	}

}
