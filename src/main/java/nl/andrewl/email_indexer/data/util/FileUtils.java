package nl.andrewl.email_indexer.data.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
	public static int getFileCount(Path dir) {
		if (!Files.isDirectory(dir)) return 0;
		try (var s = Files.list(dir)) {
			return (int) s.count();
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public static boolean isDirEmpty(Path dir) {
		return getFileCount(dir) == 0;
	}

	public static boolean dirContainsFileType(Path dir, String type) {
		try (var s = Files.walk(dir)) {
			return s.anyMatch(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(type));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
