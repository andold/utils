package kr.andold.utils.job;

import java.util.concurrent.Callable;

public interface JobInterface extends Callable<STATUS> {
	default Long getTimeout() {
		return 1L;
	}
	default STATUS call() throws Exception {
		return STATUS.NOT_SUPPORT;
	}

}
