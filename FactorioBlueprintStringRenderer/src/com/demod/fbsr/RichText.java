package com.demod.fbsr;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RichText {

	public interface Token {
	}

	public static class TextToken implements Token {
		public final String text;

		public TextToken(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return "TextToken[" + text + "]";
		}
	}

	public static class TagToken implements Token {
		public final String name;
		public final String value;

		public TagToken(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return "TagToken[name=" + name + ", value=" + value + "]";
		}
	}

	private final String raw;
	private final List<Token> tokens;

	public RichText(String raw) {
		this.raw = raw;
		this.tokens = parse(raw);
	}

	public List<Token> getTokens() {
		return tokens;
	}

	public String getRaw() {
		return raw;
	}

	private List<Token> parse(String raw) {
		List<Token> tokens = new ArrayList<>();
		Pattern pattern = Pattern.compile("\\[(?<name>[^\\]=]+)(=(?<value>[^\\]]+))?\\]");
		Matcher matcher = pattern.matcher(raw);
		int lastIndex = 0;
		while (matcher.find()) {
			if (matcher.start() > lastIndex) {
				String text = raw.substring(lastIndex, matcher.start());
				tokens.add(new TextToken(text));
			}
			String tagName = matcher.group("name");
			String tagValue = matcher.group("value");
			tokens.add(new TagToken(tagName, tagValue));
			lastIndex = matcher.end();
		}
		if (lastIndex < raw.length()) {
			tokens.add(new TextToken(raw.substring(lastIndex)));
		}
		return tokens;
	}
}
