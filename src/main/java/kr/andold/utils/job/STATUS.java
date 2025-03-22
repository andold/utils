package kr.andold.utils.job;

public enum STATUS {
	SUCCESS("성공")
	, FAILURE("실패")
		, FAIL_NO_RESULT("결과없음")
		, FAIL_NO_DATA("데이터없음")
		, FAIL_MANY_DATA("데이터여러개")
	, EXCEPTION("예외")
	, NOT_SUPPORT("지원안함")
	, INVALID("무효")
	, ALEADY_DONE("한거다")
	, RESERVED("예약")
	;
	private String title;

	private STATUS(String string) {
		title = string;
	}

	public String get() {
		return title;
	}

}
