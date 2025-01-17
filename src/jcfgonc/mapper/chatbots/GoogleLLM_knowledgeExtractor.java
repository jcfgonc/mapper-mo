package jcfgonc.mapper.chatbots;

public class GoogleLLM_knowledgeExtractor {

	public static void main(String[] args) {
		String concept = "vehicle";
		String prompt_template = "one fact per line, what are the most well known parts of a %concept% and their function? give their function with a single action verb";
		String prompt = prompt_template.replace("%concept%", concept);

		String reply = GoogleLLM_Caller.doREST_HTTP_Request(prompt);
		System.out.println(reply);
	}

	public static boolean askLLM_If_a_isa_b(String a, String b) {
		String prompt = String.format("is %s a %s%? answer yes or no and do not explain", a, b);
		String reply = GoogleLLM_Caller.doREST_HTTP_Request(prompt);
		System.out.println(reply);
		return false;
	}
}
