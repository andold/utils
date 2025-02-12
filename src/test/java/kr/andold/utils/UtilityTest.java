package kr.andold.utils;

import java.util.Date;

import org.junit.jupiter.api.Test;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UtilityTest {

	@Builder
	@Data
	public static class Clazz {
		private String title;
		private Date date;
	}

	@Test
	public void testToStringJsonObject() {
		Clazz clazz = Clazz.builder().title("Hellow").date(new Date()).build();

		log.info("{}", Utility.toStringJson(clazz));
		System.out.println(Utility.toStringJson(clazz));
	}

}
