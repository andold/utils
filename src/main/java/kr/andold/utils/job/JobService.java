package kr.andold.utils.job;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import kr.andold.utils.Utility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobService {
	private static long STARTED = System.currentTimeMillis();

	@Getter private static ConcurrentLinkedDeque<JobInterface> queue0 = new ConcurrentLinkedDeque<>();
	@Getter private static ConcurrentLinkedDeque<JobInterface> queue1 = new ConcurrentLinkedDeque<>();
	@Getter private static ConcurrentLinkedDeque<JobInterface> queue2 = new ConcurrentLinkedDeque<>();
	@Getter private static ConcurrentLinkedDeque<JobInterface> queue3 = new ConcurrentLinkedDeque<>();

	public int run() {
		log.trace("{} run()", Utility.indentStart());
		long started = System.currentTimeMillis();

		if (queue0.peek() != null) {
			JobInterface job = queue0.poll();
			int result = run(job);

			log.trace("{} 『{}/{}/{}/{}』『{}』 run() - {}", Utility.indentEnd()
					, Utility.size(queue0), Utility.size(queue1), Utility.size(queue2), Utility.size(queue3)
					, result, Utility.toStringPastTimeReadable(started));
			return result;
		}

		if (queue1.peek() != null) {
			JobInterface job = queue1.poll();
			int result = run(job);

			log.trace("{} 『{}/{}/{}/{}』『{}』 run() - {}", Utility.indentEnd()
					, Utility.size(queue0), Utility.size(queue1), Utility.size(queue2), Utility.size(queue3)
					, result, Utility.toStringPastTimeReadable(started));
			return result;
		}
		
		if (queue2.peek() != null) {
			JobInterface job = queue2.poll();
			int result = run(job);

			log.trace("{} 『{}/{}/{}/{}』『{}』 run() - {}", Utility.indentEnd()
					, Utility.size(queue0), Utility.size(queue1), Utility.size(queue2), Utility.size(queue3)
					, result, Utility.toStringPastTimeReadable(started));
			return result;
		}
		
		if (queue3.peek() != null) {
			JobInterface job = queue3.poll();
			int result = run(job);

			log.trace("{} 『{}/{}/{}/{}』『{}』 run() - {}", Utility.indentEnd()
					, Utility.size(queue0), Utility.size(queue1), Utility.size(queue2), Utility.size(queue3)
					, result, Utility.toStringPastTimeReadable(started));
			return result;
		}
		
		log.trace("{} 『{}/{}/{}/{}』『EMPTY::{}』 run() - {}", Utility.indentEnd()
				, Utility.size(queue0), Utility.size(queue1), Utility.size(queue2), Utility.size(queue3)
				, -1, Utility.toStringPastTimeReadable(started));
		return -1;
	}

	private int run(JobInterface job) {
		log.trace("{} run({})", Utility.indentStart(), job);
		long started = System.currentTimeMillis();

		if (job == null) {
			log.trace("{} 『NULL{}』 run({}) - {}", Utility.indentEnd(), -1, job, Utility.toStringPastTimeReadable(started));
			return -1;
		}

		ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<STATUS> future = executor.submit(job);
		STATUS result = STATUS.EXCEPTION;
		try {
	        result = future.get(job.getTimeout(), TimeUnit.SECONDS);
			executor.shutdown();
		} catch (Exception e) {
			log.error("{} run({}) - Exception::{}", Utility.indentMiddle(), job, e.getLocalizedMessage(), e);
			future.cancel(true);
			executor.shutdownNow();
		}

		log.trace("{} 『{}』 run({}) - {}", Utility.indentEnd(), result, job, Utility.toStringPastTimeReadable(started));
		return -1;
	}

	public void status() {
		log.info("{} 『{}/{}/{}/{}』 status() - {}", Utility.indentMiddle(), Utility.size(queue0), Utility.size(queue1), Utility.size(queue2), Utility.size(queue3), Utility.toStringPastTimeReadable(STARTED));
	}

}
