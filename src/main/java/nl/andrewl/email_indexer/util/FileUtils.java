package nl.andrewl.email_indexer.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

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

	public static void deleteFiles(Path dir) {
		try (var s = Files.walk(dir)) {
			s.filter(Files::isRegularFile).forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void deleteDirectoryRecursive(Path dir) throws IOException {
		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public static Path getClasspathResourceAsPath(String name) {
		URL url = Thread.currentThread().getContextClassLoader().getResource(name);
		if (url == null) return null;
		try {
			return Path.of(url.toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
