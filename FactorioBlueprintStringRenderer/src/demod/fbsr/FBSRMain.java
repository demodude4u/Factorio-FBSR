package demod.fbsr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.json.JSONException;
import org.json.JSONObject;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.ResourceFinder;
import org.luaj.vm2.lib.jse.JsePlatform;

import demod.fbsr.render.Renderer;
import demod.fbsr.render.TypeRendererFactory;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class FBSRMain {

	public static final String DEFAULT_FACTORIO = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\Factorio";
	public static final String DEFAULT_LUA = "lua";
	private static final String TYPE_SCHEMA = "https://raw.githubusercontent.com/jcranmer/factorio-tools/master/schema.json";

	private static Map<String, BufferedImage> modImageCache = new HashMap<>();
	private static File factorio;

	private static List<Point2D.Double> debugPoints = new ArrayList<>();

	public static synchronized void debugPoint(Point2D.Double point) {
		debugPoints.add(point);
	}

	private static String decode(String blueprint) throws IOException {
		byte[] bin = Base64.getDecoder().decode(blueprint);
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(bin))))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		}
	}

	public static BufferedImage getModImage(LuaValue value) {
		String path = value.toString();
		return modImageCache.computeIfAbsent(path, p -> {
			String firstSegment = path.split("\\/")[0];
			String mod = firstSegment.substring(2, firstSegment.length() - 2);
			File modFolder = new File(factorio, "data/" + mod);
			try {
				return ImageIO.read(new File(modFolder, path.replace(firstSegment, "").substring(1)));
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		});
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		setupWorkingDirectory();

		OptionParser optionParser = new OptionParser();
		optionParser.accepts("?", "Shows help.").forHelp();
		OptionSpec<File> factorioOption = optionParser.accepts("factorio", "Location of the factorio folder.")
				.withRequiredArg().ofType(File.class).defaultsTo(new File(DEFAULT_FACTORIO));
		OptionSpec<File> luaOption = optionParser.accepts("lua", "Location of the needed lua files.").withRequiredArg()
				.ofType(File.class).defaultsTo(new File(DEFAULT_LUA));
		OptionSpec<File> inputOption = optionParser.accepts("input", "Location of the blueprint file.")
				.withRequiredArg().ofType(File.class);
		OptionSpec<File> outputOption = optionParser.accepts("output", "Location to save the output PNG file.")
				.withRequiredArg().ofType(File.class).defaultsTo(new File("output.png"));
		optionParser.accepts("show", "Open image immediately after being saved.");

		OptionSet options = null;
		try {
			options = optionParser.parse(args);
		} catch (UnsupportedOperationException e1) {
			System.err.println(e1.getMessage());
			optionParser.printHelpOn(System.err);
			System.exit(0);
		}
		if (options.has("?")) {
			optionParser.printHelpOn(System.out);
			System.exit(0);
		}

		factorio = options.valueOf(factorioOption);
		File lua = options.valueOf(luaOption);
		boolean hasInput = options.has(inputOption);
		File inputBlueprint = hasInput ? options.valueOf(inputOption) : null;
		File outputImage = options.valueOf(outputOption);
		boolean showImage = options.has("show");

		File[] luaFolders = new File[] { //
				lua, //
				new File(factorio, "data/core/luaLib"), //
				new File(factorio, "data"), //
				new File(factorio, "data/core"), //
				new File(factorio, "data/base"), //
				new File(lua, "blueprint-string_4.0.0"), //
				new File(lua, "blueprint-string_4.0.0/blueprintstring"),//
		};

		String luaPath = Arrays.stream(luaFolders).map(f -> f.getAbsolutePath() + File.separator + "?.lua")
				.collect(Collectors.joining(";")).replace('\\', '/');
		// System.out.println("LUA_PATH: " + luaPath);

		Globals globals = JsePlatform.standardGlobals();
		globals.load(new BaseLib());
		globals.load(new DebugLib());
		globals.load(new StringReader("package.path = package.path .. ';" + luaPath + "'"), "initLuaPath").call();
		globals.finder = new ResourceFinder() {
			@Override
			public InputStream findResource(String filename) {
				File file = new File(filename);
				// System.out.println(filename + "? " + file.exists());
				try {
					return file.exists() ? new FileInputStream(file) : null;
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		};

		File blueprintFile = inputBlueprint;
		if (!hasInput) {
			String[] testBlueprintFiles = Stream.of(new File(".").listFiles()).map(File::getName)
					.filter(n -> n.endsWith(".blueprint")).toArray(String[]::new);
			if (testBlueprintFiles.length == 0) {
				System.err.println(
						"You have not specified an input file, and there aren't any test files in this folder!");
				System.exit(-1);
			}
			Object choice = JOptionPane.showInputDialog(null, "Test file:", "FBSR", JOptionPane.QUESTION_MESSAGE, null,
					testBlueprintFiles, testBlueprintFiles[0]);
			if (choice == null) {
				return;
			}
			blueprintFile = new File(choice.toString());
		}

		String blueprintEncoded = Files.readAllLines(blueprintFile.toPath()).stream().collect(Collectors.joining());
		String blueprintDecoded = decode(blueprintEncoded);
		// System.out.println(blueprintDecoded);

		TypeHiearchy typeHiearchy = new TypeHiearchy(readJsonFromStream(new URL(TYPE_SCHEMA).openStream()));

		FBSRBridge.setBlueprintDecoded(blueprintDecoded);
		FBSRBridge.setBlueprintListener(blueprint -> {
			List<BlueprintEntity> blueprintEntities = new ArrayList<>();
			Utils.forEach(blueprint.get("entities").checktable(),
					v -> blueprintEntities.add(new BlueprintEntity(v.checktable())));

			DataTable dataTable = new DataTable(typeHiearchy, globals.get("data").checktable());
			Map<String, DataPrototype> entities = dataTable.getEntities();

			class RenderingTuple {
				BlueprintEntity entity;
				DataPrototype prototype;
				TypeRendererFactory factory;
			}
			List<RenderingTuple> renderingTuples = new ArrayList<RenderingTuple>();
			for (BlueprintEntity entity : blueprintEntities) {
				RenderingTuple tuple = new RenderingTuple();
				tuple.entity = entity;
				tuple.prototype = entities.get(entity.getName());
				if (tuple.prototype == null) {
					System.err.println("Cant find prototype for " + entity.getName());
					continue;
				}
				tuple.factory = TypeRendererFactory.forType(tuple.prototype.getType());
				renderingTuples.add(tuple);
			}

			WorldMap worldState = new WorldMap();
			renderingTuples.forEach(t -> t.factory.populateWorldMap(worldState, dataTable, t.entity, t.prototype));

			List<Renderer> renderers = new ArrayList<>();
			renderingTuples.forEach(
					t -> t.factory.createRenderers(renderers::add, worldState, dataTable, t.entity, t.prototype));

			BufferedImage render = renderImage((int) Math.round(TypeRendererFactory.tileSize), renderers);
			try {
				ImageIO.write(render, "PNG", outputImage);
				System.out.println(outputImage.getAbsolutePath());
				if (showImage) {
					Desktop.getDesktop().open(outputImage);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		globals.load(new FileReader(new File(lua, "fbsr.lua")), "fbsr").call();
	}

	@SuppressWarnings("resource")
	private static JSONObject readJsonFromStream(InputStream in) throws JSONException, IOException {
		return new JSONObject(new Scanner(in, "UTF-8").useDelimiter("\\A").next());
	}

	private static BufferedImage renderImage(int tileSize, List<Renderer> renderers) {
		Area area = new Area();
		renderers.forEach(r -> area.add(new Area(r.getBounds())));
		Rectangle2D bounds = area.getBounds2D();
		bounds.setFrameFromDiagonal(Math.floor(bounds.getMinX()) - 1, Math.floor(bounds.getMinY()) - 1,
				Math.ceil(bounds.getMaxX()) + 1, Math.ceil(bounds.getMaxY()) + 1);
		BufferedImage image = new BufferedImage((int) (bounds.getWidth() * tileSize),
				(int) (bounds.getHeight() * tileSize), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.scale(image.getWidth() / bounds.getWidth(), image.getHeight() / bounds.getHeight());
		g.translate(-bounds.getX(), -bounds.getY());
		g.setColor(new Color(40, 40, 40));
		g.fill(bounds);
		g.setStroke(new BasicStroke((float) (3 / TypeRendererFactory.tileSize)));
		g.setColor(new Color(60, 60, 60));
		g.setFont(new Font("Courier New", Font.PLAIN, 1).deriveFont(0.3f));
		for (double x = Math.round(bounds.getMinX()); x <= bounds.getMaxX(); x++) {
			g.draw(new Line2D.Double(x, bounds.getMinY(), x, bounds.getMaxY()));
			g.drawString("" + (int) x, (float) x + 0.1f, (float) (bounds.getMinY() + 0.35f));
		}
		for (double y = Math.round(bounds.getMinY()); y <= bounds.getMaxY(); y++) {
			g.draw(new Line2D.Double(bounds.getMinX(), y, bounds.getMaxX(), y));
			g.drawString("" + (int) y, (float) (bounds.getMinX() + 0.1f), (float) y + 0.35f);
		}

		renderers.stream().sorted((r1, r2) -> {
			int ret;

			Rectangle2D.Double b1 = r1.getBounds();
			Rectangle2D.Double b2 = r2.getBounds();

			ret = Double.compare(b1.getMinY(), b2.getMinY());
			if (ret != 0) {
				return ret;
			}

			ret = Double.compare(b1.getMinX(), b2.getMinX());
			if (ret != 0) {
				return ret;
			}

			ret = r1.getLayer().compareTo(r2.getLayer());
			return ret;
		}).forEach(r -> r.render(g));

		g.setColor(Color.magenta);
		for (Point2D.Double debugPoint : debugPoints) {
			g.draw(new Ellipse2D.Double(debugPoint.x - 0.25, debugPoint.y - 0.25, 0.5, 0.5));
		}

		g.dispose();
		return image;
	}

	private static void setupWorkingDirectory() {
		String className = FBSRMain.class.getName().replace('.', '/');
		String classJar = FBSRMain.class.getResource("/" + className + ".class").toString();
		if (classJar.startsWith("jar:")) {
			try {
				File jarFolder = new File(
						FBSRMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
								.getParentFile();
				// System.out.println("Jar Folder: " +
				// jarFolder.getAbsolutePath());
				System.setProperty("user.dir", jarFolder.getAbsolutePath());
			} catch (URISyntaxException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
