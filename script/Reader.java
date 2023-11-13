import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

// TODO fix names after decompile from cache
public class Reader {
	public static Class<?> stdDraw;
	private static JFrame pane = null;
	private static int unitCounter = 0;

	static {
		try {
			Class<?> draw = Class.forName("StdDraw");
			stdDraw = draw;
			resetStdDraw();
			// If we use a real StdDraw file we need to uncomment these
			Field frame = stdDraw.getDeclaredField("frame");
			frame.setAccessible(true);
			pane = (JFrame) frame.get((Object) null);
			if (pane != null) {
				pane.setVisible(false);
				pane.dispose();
			}
		} catch (ClassNotFoundException var2) {
			System.err.println("StdDraw was not found on the Classpath.");
			System.exit(1);
		} catch (NoSuchFieldException e) {
			// just ignore this one
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		} 

	}

	public Reader() {
	}

	public static void main(String[] args) {
		System.err.println(Arrays.toString(args));
		Stream.of(args).map(ExecUnit::new).forEach(Reader::exec);
		System.err.println("Dispatched Event: Exit");
		if (pane != null) {
			pane.dispatchEvent(new WindowEvent(pane, 201));
			pane.dispose();
		}
		System.err.println("Generated images for %d out of %d Units".formatted(unitCounter, args.length));
	}

	private static Class<?> loadClass(URI path, String name) {
		try (URLClassLoader loader = URLClassLoader.newInstance(new URL[] { path.toURL() })) {
			Class<?> c = loader.loadClass(name);
			System.err.println("Loaded class for Path: " + String.valueOf(path));
			return c;
		} catch (IOException var16) {
			System.err.println("ERROR: Could not load classes in Path: " + String.valueOf(path));
		} catch (ClassNotFoundException var17) {
			System.err.println("ERROR: Could not find class Ship in Path: " + String.valueOf(path));
		}

		return null;
	}

	private static void exec(ExecUnit unit) {
		if (unit.clazz != null && unit.fileName != null) {
			try {
				Method m = unit.clazz.getMethod("main", String[].class);
				System.err.println("Executing Main Method for Unit");
				Thread t = new Thread(() -> {
					try {
						m.invoke(null, (Object)null);
					} catch (InvocationTargetException e) {
						System.err.println("Main Method invokation has failed. You can ignore this");
						System.err.println(e.getCause().getMessage());
						return;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						System.err.println("Main Method invokation has failed for unknown reasons");
						e.printStackTrace(System.err);
						return;
					}
				});
				t.start();
				t.join(2000);
				t.stop();

				Field image = stdDraw.getDeclaredField("onscreenImage");
				image.setAccessible(true);

				BufferedImage img = (BufferedImage) image.get((Object) null);
				System.err.println("Writing Image:");
		//		System.err.println(img);
				System.err.println("Writing to: " + unit.path + unit.fileName + ".png");
				File output = new File(unit.path + unit.fileName + ".png");
				output.createNewFile();
				boolean success = ImageIO.write(img, "png", output);
				if (!success) {
					System.err.println("Failed to write Image");
				}
				System.err.println("""

					Finished Execution for Unit %s
					Reset StdDraw	
				-----------------------------------------
				""".formatted(unit.fileName));
				resetStdDraw();
				++unitCounter;
				System.out.println(unit.path);
			} catch (Exception e) {
				System.err.println("ERROR:" + String.valueOf(e));
				e.printStackTrace();
			}

		}
	}

	private static void resetStdDraw() {
		try {
			// TODO i could just have called the init Method for most of this couldnt I...
			stdDraw.getDeclaredMethod("setXscale", (Class[]) null).invoke((Object) null, (Object[]) null);
			stdDraw.getDeclaredMethod("setYscale", (Class[]) null).invoke((Object) null, (Object[]) null);
			stdDraw.getDeclaredMethod("setPenColor", (Class[]) null).invoke((Object) null, (Object[]) null);
			stdDraw.getDeclaredMethod("setPenRadius", (Class[]) null).invoke((Object) null, (Object[]) null);
			stdDraw.getDeclaredMethod("setFont", (Class[]) null).invoke((Object) null, (Object[]) null);
			stdDraw.getDeclaredMethod("clear", (Class[]) null).invoke((Object) null, (Object[]) null);
			Field defer = stdDraw.getDeclaredField("defer");
			defer.setAccessible(true);
			defer.set((Object) null, false);
			Field mousePressed = stdDraw.getDeclaredField("mousePressed");
			mousePressed.setAccessible(true);
			mousePressed.set((Object) null, false);
			Field nextDraw = stdDraw.getDeclaredField("nextDraw");
			nextDraw.setAccessible(true);
			nextDraw.set((Object) null, -1);
			Field keysTyped = stdDraw.getDeclaredField("keysTyped");
			keysTyped.setAccessible(true);
			keysTyped.set((Object) null, new LinkedList());
			Field keysDown = stdDraw.getDeclaredField("keysDown");
			keysDown.setAccessible(true);
			keysDown.set((Object) null, new TreeSet());
		} catch (Exception var5) {
			var5.printStackTrace();
		}

	}

	static class ExecUnit {
		String fileName = "drawing";
		Class<?> clazz = null;
		String path = "";

		ExecUnit(String path) {
			this.path = path;
			this.clazz = Reader.loadClass(URI.create("file:" + path), "Ship");
		}

		public String toString() {
			String var10000 = this.fileName;
			return "ExecUnit{fileName=" + var10000 + ", clazz=" + String.valueOf(this.clazz) + "}";
		}
	}

}
