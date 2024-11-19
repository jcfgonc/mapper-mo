package jcfgonc.mapper.chatbots.json;

import java.util.ArrayList;

public class Content {
	public ArrayList<Part> parts;
	public String role;

	@Override
	public String toString() {
		return "parts=[" + parts + "], role=" + role;
	}
}
