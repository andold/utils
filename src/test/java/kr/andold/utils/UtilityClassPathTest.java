package kr.andold.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lombok.Data;

//	#11 readClassPathFile() - Windows/Gradle 환경에서 classpath 파일을 찾지 못함
class UtilityClassPathTest {

	@Data
	public static class Line {
		private String title;
	}

	@Test
	void readClassPathFile() {
		assertEquals("안녕하세요\nhello classpath\n", Utility.readClassPathFile("utility/classpath-file.txt"));
		assertEquals("안녕하세요\nhello classpath\n", Utility.readClassPathFile("/utility/classpath-file.txt"));
	}

	@Test
	void readClassPathFileNotFound() {
		assertNull(Utility.readClassPathFile("utility/not-exist.txt"));
		assertNull(Utility.readClassPathFile(null));
	}

	@Test
	void readClassPathFileInJar(@TempDir Path dir) throws Exception {
		Path jar = dir.resolve("resource.jar");
		try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
			out.putNextEntry(new JarEntry("utility/in-jar.txt"));
			out.write("jar 안의 리소스\n".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}

		Thread thread = Thread.currentThread();
		ClassLoader previous = thread.getContextClassLoader();
		try (URLClassLoader classLoader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, previous)) {
			thread.setContextClassLoader(classLoader);
			assertEquals("jar 안의 리소스\n", Utility.readClassPathFile("utility/in-jar.txt"));
		} finally {
			thread.setContextClassLoader(previous);
		}
	}

	@Test
	void readClassPathExcelFile() {
		String string = Utility.readClassPathExcelFile("utility/classpath-excel.xls");
		assertNotNull(string);
		assertTrue(string.contains("이름\t값\t"), string);
		assertNull(Utility.readClassPathExcelFile("utility/not-exist.xls"));
	}

	@Test
	void parseJsonLines() {
		List<Line> lines = Utility.parseJsonLines("utility/classpath-lines.jsonl", Line.class, Integer.MAX_VALUE);
		assertNotNull(lines);
		assertEquals(2, lines.size());
		assertEquals("둘째", lines.get(1).getTitle());
		assertNull(Utility.parseJsonLines("utility/not-exist.jsonl", Line.class, Integer.MAX_VALUE));
	}

}
