package com.demod.fbsr;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.demod.fbsr.AtlasManager.AtlasRef;
import com.demod.fbsr.def.IconDef;
import com.demod.fbsr.map.MapRect;

public class RichText {

	private static final Pattern TOKEN_PATTERN = Pattern.compile("\\[(?<name>/?[^\\]=]+)(=(?<value>[^\\]]*?)(,quality=(?<quality>[^\\]]+))?)?\\]");

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
		public final Optional<String> quality;

		public TagToken(String name, String value, Optional<String> quality) {
			this.name = name;
			this.value = value;
			this.quality = quality;
		}

		public String formatted() {
			return "[" + name + (value != null ? "=" + value : "") +
				(quality.isPresent() ? ",quality=" + quality.get() : "") + "]";
		}

		@Override
		public String toString() {
			return "TagToken[name=" + name + ", value=" + value + 
				quality.map(q -> ", quality=" + q).orElse("") + "]";
		}
	}

	public static class ClosingTagToken implements Token {
		public final String name;

		public ClosingTagToken(String name) {
			this.name = name.substring(1); // Remove the leading '/'
		}

		@Override
		public String toString() {
			return "ClosingTagToken[name=" + name + "]";
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
		Matcher matcher = TOKEN_PATTERN.matcher(raw);
		int lastIndex = 0;
		while (matcher.find()) {
			if (matcher.start() > lastIndex) {
				String text = raw.substring(lastIndex, matcher.start());
				if (!text.isEmpty()) {
					tokens.add(new TextToken(text));
				}
			}
			String tagName = matcher.group("name");
			if (tagName.startsWith("/")) {
				tokens.add(new ClosingTagToken(tagName));
			} else {
				String tagValue = matcher.group("value");
				String quality = matcher.group("quality");
				tokens.add(new TagToken(tagName, tagValue, Optional.ofNullable(quality)));
			}
			lastIndex = matcher.end();
		}
		if (lastIndex < raw.length()) {
			tokens.add(new TextToken(raw.substring(lastIndex)));
		}
		return tokens;
	}

	private static final float ICON_GAP = 0.1f;
	private static final float ICON_SIZE = 1.25f;
	private static final float ICON_OFFSET_Y = 0.175f;

	public void draw(Graphics2D g, double x, double y) {
		AffineTransform pat = g.getTransform();
		try {
			Font font = g.getFont();
			FontRenderContext frc = g.getFontRenderContext();
			float tagSize = font.getSize();

			for (Token token : tokens) {
				if (token instanceof TextToken) {
					TextToken textToken = (TextToken) token;
					g.drawString(textToken.text, (float) x, (float) y);
					TextLayout layout = new TextLayout(textToken.text, font, frc);
					x += layout.getAdvance();
				
				} else if (token instanceof TagToken) {
					TagToken tagToken = (TagToken) token;

					if (tagToken.name.equals("color") || tagToken.name.equals("font")) {
						continue; //TODO handle color and font changes
					}

					Optional<TagWithQuality> lookupTag = IconManager.lookupTag(tagToken);
					if (lookupTag.isPresent()) {
						TagWithQuality tag = lookupTag.get();
						Optional<String> quality = tag.getQuality();
						AtlasRef ref = tag.getDef().getAtlasRef();
						Image image = ref.getAtlas().getBufferedImage();
						Rectangle source = ref.getRect();
						
						double iconY = y - tagSize / 2.0 + (ICON_OFFSET_Y - ICON_SIZE / 2.0) * tagSize;
						g.translate(x, iconY);
						g.scale(ICON_SIZE * tagSize, ICON_SIZE * tagSize);
						
						g.drawImage(image, 0, 0, 1, 1, source.x, source.y, source.x + source.width, source.y + source.height, null);

						if (quality.isPresent()) {
							Optional<IconDef> def = IconManager.lookupQuality(quality.get());
							double qSize = 0.4;
							g.translate(0, 1.0 - qSize);
							g.scale(qSize, qSize);
							AtlasRef qRef = def.get().getAtlasRef();
							Image qImage = qRef.getAtlas().getBufferedImage();
							Rectangle qSource = qRef.getRect();
							g.drawImage(qImage, 0, 0, 1, 1, qSource.x, qSource.y, qSource.x + qSource.width, qSource.y + qSource.height,
									null);
						}

						g.setTransform(pat);

						x += tagSize * (ICON_GAP + ICON_SIZE + ICON_GAP);
					} else {
						x += ICON_GAP * tagSize;
					}

				} else if (token instanceof ClosingTagToken) {
					// TODO color and font stack handling
				}
			}

		} finally {
			g.setTransform(pat);
		}
	}

	public double getTextWidth(Graphics2D g) {
		Font font = g.getFont();
		FontRenderContext frc = g.getFontRenderContext();
		float tagSize = font.getSize();
		double width = 0.0;

		for (Token token : tokens) {
			if (token instanceof TextToken) {
				TextToken textToken = (TextToken) token;
				TextLayout layout = new TextLayout(textToken.text, font, frc);
				width += layout.getAdvance();

			} else if (token instanceof TagToken) {
				TagToken tagToken = (TagToken) token;
				Optional<TagWithQuality> lookupTag = IconManager.lookupTag(tagToken);
				if (lookupTag.isPresent()) {
					width += tagSize * (ICON_GAP + ICON_SIZE + ICON_GAP);
				} else {
					width += ICON_GAP * tagSize;
				}
			}
		}
		return width;
	}
}
