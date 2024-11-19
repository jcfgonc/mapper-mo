package jcfgonc.mapper.chatbots.json;

public class Candidate {
	public Content content;
	public String finishReason;
	public double avgLogprobs;

	@Override
	public String toString() {
		return "content=" + content + ", finishReason=" + finishReason + ", avgLogprobs=" + avgLogprobs;
	}
}
