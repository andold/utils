package kr.andold.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChromeDriverWrapper extends ChromeDriver {
	private static final int PAUSE = 100;
	private WebDriverWait wait;

	public static ChromeDriverWrapper defaultChromeDriver(String webdriverPath, String userDataDir) {
		log.info("{} defaultChromeDriver(『{}』, 『{}』)", Utility.indentStart(), webdriverPath, userDataDir);
		long started = System.currentTimeMillis();

		System.setProperty("webdriver.chrome.driver", webdriverPath);
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--remote-allow-origins=*");
		options.addArguments("--incognito");
		options.addArguments("--window-size=1024,768");
		options.addArguments(String.format("--user-data-dir=%s", userDataDir));

		//options.addArguments("--start-maximized");
		options.addArguments("--disable-blink-features=AutomationControlled");
		options.addArguments("--disable-dev-shm-usage");
		//options.addArguments("--disable-extensions");
		options.addArguments("--disable-gpu");
		options.addArguments("--disable-infobars");
		options.addArguments("--no-sandbox");
		options.setPageLoadStrategy(PageLoadStrategy.NONE);
		
		ChromeDriverWrapper driver = new ChromeDriverWrapper(options);
		driver.wait = new WebDriverWait(driver, Duration.ofSeconds(8));

		log.info("{} {} defaultChromeDriver(『{}』, 『{}』) - {}", Utility.indentEnd(), "driver", webdriverPath, userDataDir, Utility.toStringPastTimeReadable(started));
		return driver;
	}

	public ChromeDriverWrapper() {
		super();
		this.wait = new WebDriverWait(this, Duration.ofSeconds(8));
	}
	public ChromeDriverWrapper(ChromeOptions options) {
		super(options);
		this.wait = new WebDriverWait(this, Duration.ofSeconds(8));
	}

	public boolean setInputValue(By by, String value) {
		try {
			WebElement e = findElement(by);
			if (e == null) {
				return false;
			}

			JavascriptExecutor jsExecutor = (JavascriptExecutor)this;
			jsExecutor.executeScript("arguments[0].value = \"" + value + "\"", e);

			return true;
		} catch (Exception e) {
		}

		return false;
	}

	@Deprecated
	public WebElement findElement(By xpath, int milli) throws Exception {
		log.info("{} findElement(..., {})", Utility.indentStart(), milli);
		long started = System.currentTimeMillis();
		long end = started + milli;

		Exception previous = null;
		while (System.currentTimeMillis() < end) {
			try {
				WebElement e = super.findElement(xpath);

				log.info("{} {} findElement(..., {}) - {}", Utility.indentEnd(), "SUCCESS", milli, Utility.toStringPastTimeReadable(started));
				return e;
			} catch (Exception e) {
				previous = e;
			}
			Utility.sleep(PAUSE);
		}

		log.info("{} {} findElement(..., {}) - {}", Utility.indentEnd(), "FAILURE", milli, Utility.toStringPastTimeReadable(started));
		throw previous;
	}

	public WebElement findElement(By by, Duration duration) throws Exception {
		Exception previous = null;

		try {
			WebElement result = null;
			Duration durationPrevious = manage().timeouts().getImplicitWaitTimeout();
			manage().timeouts().implicitlyWait(duration);

			try {
				result = findElement(by);
			} catch (Exception e) {
				previous = e;
			}

			manage().timeouts().implicitlyWait(durationPrevious);
			if (previous == null) {
				return result;
			}
		} catch (Exception e) {
			previous = e;
		}

		throw previous;
	}

	public WebElement findElement(By xpath, int milli, String... marks) throws Exception {
		Exception exception = null;
		WebElement element = null;
		while (milli >= 0) {
			try {
				element = super.findElement(xpath);
				String text = element.getText();
				boolean found = false;
				for (String mark : marks) {
					if (mark.contentEquals(text)) {
						found = true;
						break;
					}
				}

				if (found) {
					Utility.sleep(PAUSE);
					milli -= PAUSE;
					continue;
				}

				return element;
			} catch (Exception e) {
				exception = e;
			}
			Utility.sleep(PAUSE);
			milli -= PAUSE;
		}

		if (element == null && exception == null) {
			return null;
		}

		if (exception == null) {
			return element;
		}

		throw exception;
	}

	public WebElement findElement(WebElement element, By xpath, int timeout) {
		while (timeout > 0) {
			try {
				return element.findElement(xpath);
			} catch (Exception e) {
			}
			Utility.sleep(PAUSE);
			timeout -= PAUSE;
		}
		return null;
	}

	public boolean waitUntilTextMatch(By xpath, String pattern) {
		log.info("{} waitUntilTextMatch(..., 『{}』)", Utility.indentStart(), pattern);
		long started = System.currentTimeMillis();

		try {
			wait.until(ExpectedConditions.textMatches(xpath, Pattern.compile(pattern)));
			log.info("{} {} waitUntilTextMatch(..., 『{}』) - {}", Utility.indentEnd(), true, pattern, Utility.toStringPastTimeReadable(started));
			return true;
		} catch (Exception e) {
		}

		log.info("{} {} waitUntilTextMatch(..., 『{}』) - {}", Utility.indentEnd(), false, pattern, Utility.toStringPastTimeReadable(started));
		return false;
	}

	public boolean waitUntilTextNotInclude(By xpath, int milli, String... marks) throws Exception {
		log.info("{} waitUntilTextNotInclude(..., {}, 『{}』)", Utility.indentStart(), milli, Utility.ellipsisEscape(marks, 16));
		long started = System.currentTimeMillis();

		while (milli >= 0) {
			try {
				boolean found = false;
				String text = getText(xpath, 1, "waitUntilTextInclude");
				for (String mark : marks) {
					if (mark.length() == 0 && text.length() > 0) {
						log.info("{} {} waitUntilTextNotInclude(..., {}, 『{}』) - {}", Utility.indentEnd(), true, milli, "marks", Utility.toStringPastTimeReadable(started));
						return true;
					}
					if (text.contains(mark)) {
						found = true;
						break;
					}
				}

				if (found) {
					Utility.sleep(PAUSE);
					milli -= PAUSE;
					continue;
				}

				log.info("{} {} waitUntilTextNotInclude(..., {}, 『{}』) - {}", Utility.indentEnd(), true, milli, "marks", Utility.toStringPastTimeReadable(started));
				return true;
			} catch (Exception e) {
			}
			Utility.sleep(PAUSE);
			milli -= PAUSE;
		}

		log.info("{} {} waitUntilTextNotInclude(..., {}, 『{}』) - {}", Utility.indentEnd(), false, milli, "marks", Utility.toStringPastTimeReadable(started));
		return false;
	}

	public boolean waitUntilTextNotBlank(By xpath, int milli) {
		log.info("{} waitUntilTextNotBlank(..., {})", Utility.indentStart(), milli);
		long started = System.currentTimeMillis();

		while (milli >= 0) {
			try {
				String text = getText(xpath, 1, "waitUntilTextNotBlank");
				if (text.isBlank()) {
					Utility.sleep(PAUSE);
					milli -= PAUSE;
					continue;
				}

				log.info("{} {} waitUntilTextNotBlank(..., {}) - {}", Utility.indentEnd(), true, milli, Utility.toStringPastTimeReadable(started));
				return true;
			} catch (Exception e) {
			}
		}

		log.info("{} {} waitUntilTextNotBlank(..., {}, 『{}』) - {}", Utility.indentEnd(), false, milli, "marks", Utility.toStringPastTimeReadable(started));
		return false;
	}

	public boolean waitUntilTextInclude(By xpath, int milli, String... marks) throws Exception {
		log.info("{} waitUntilTextInclude(..., {}, 『{}』)", Utility.indentStart(), milli, "marks");
		long started = System.currentTimeMillis();

		while (milli >= 0) {
			try {
				String text = getText(xpath, 1, "waitUntilTextInclude");
				for (String mark : marks) {
					if (text.contains(mark)) {
						log.info("{} {} waitUntilTextInclude(..., {}, 『{}』) - {}", Utility.indentEnd(), true, milli, "marks", Utility.toStringPastTimeReadable(started));
						return true;
					}
				}
			} catch (Exception e) {
			}
			Utility.sleep(PAUSE);
			milli -= PAUSE;
		}

		log.info("{} {} waitUntilTextInclude(..., {}, 『{}』) - {}", Utility.indentEnd(), false, milli, "marks", Utility.toStringPastTimeReadable(started));
		return false;
	}

	public WebElement findElementIncludeText(By xpath, int milli, String string) throws Exception {
		Exception exception = null;
		while (milli >= 0) {
			try {
				List<WebElement> elements = super.findElements(xpath);
				for (WebElement e : elements) {
					String text = e.getText();
					if (text.indexOf(string) >= 0) {
						return e;
					}
				}

				Utility.sleep(PAUSE);
				milli -= PAUSE;
			} catch (Exception e) {
				exception = e;
			}
		}

		if (exception != null) {
			throw exception;
		}

		throw new Exception();
	}

	public WebElement findElementIncludeTextAndClass(By xpath, int milli, String string, String clazz) throws Exception {
		Exception exception = null;
		while (milli >= 0) {
			try {
				List<WebElement> elements = super.findElements(xpath);
				for (WebElement e : elements) {
					String text = e.getText();
					String attribute = e.getAttribute("class");
					if (text.indexOf(string) >= 0 && attribute.indexOf(clazz) >= 0) {
						return e;
					}

					Utility.sleep(PAUSE);
					milli -= PAUSE;
				}

			} catch (Exception e) {
				exception = e;
			}
		}

		throw exception;
	}

	public boolean clickIncludeTextInAttribute(By xpath, int timeout, String attrName, String text) {
		while (timeout > 0) {
			try {
				List<WebElement> elements = super.findElements(xpath);
				for (WebElement e : elements) {
					String attribute = e.getAttribute(attrName);
					if (attribute.indexOf(text) >= 0) {
						e.click();
						return true;
					}

					Utility.sleep(PAUSE);
					timeout -= PAUSE;
				}

			} catch (Exception e) {
			}
		}
		return false;
	}

	public static int compareFloat(Float left, Float right, Float epsilon) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return -1;
		}
		if (right == null) {
			return 1;
		}
		
		Float delta = Math.abs(left - right);
		if (delta < epsilon) {
			return 0;
		}
		
		if (delta < 0) {
			return -1;
		}

		return 1;
	}

	public Boolean domAttributeToBe(By by, String attribute, String pattern, Duration duration) {
		log.trace("{} domAttributeToBe(..., 『{}』, 『{}』, ...)", Utility.indentStart(), attribute, pattern);
		long started = System.currentTimeMillis();

		try {
			WebElement element = findElement(by, duration);
			WebDriverWait wait = new WebDriverWait(this, duration);
			Boolean result = wait.until(ExpectedConditions.domAttributeToBe(element, attribute, pattern));

			log.trace("{} {} domAttributeToBe(..., 『{}』, 『{}』, ...) - {}", Utility.indentEnd(), result, attribute, pattern, Utility.toStringPastTimeReadable(started));
			return result;
		} catch (Exception e) {
		}

		log.trace("{} {} domAttributeToBe(..., 『{}』, 『{}』, ...) - {}", Utility.indentEnd(), false, attribute, pattern, Utility.toStringPastTimeReadable(started));
		return false;
	}

	@Deprecated
	public List<WebElement> findElements(By xpath, int milli) throws Exception {
		List<WebElement> elements = null;
		while (milli >= 0) {
			try {
				elements = super.findElements(xpath);
				if (!elements.isEmpty()) {
					return elements;
				}
			} catch (Exception e) {
			}
			Utility.sleep(PAUSE);
			milli -= PAUSE;
		}
		return elements;
	}

	public List<WebElement> findElements(By by, Duration duration) {
		List<WebElement> result = new ArrayList<>();

		try {
			Duration durationPrevious = manage().timeouts().getImplicitWaitTimeout();
			this.manage().timeouts().implicitlyWait(duration);

			try {
				result = findElements(by);
			} catch (Exception e) {
			}

			manage().timeouts().implicitlyWait(durationPrevious);
		} catch (Exception e) {
		}

		return result;
	}

	public void clear(By xpath) {
		try {
			List<WebElement> es = findElements(xpath);
			if (es == null || es.isEmpty()) {
				return;
			}

			JavascriptExecutor jsExecutor = (JavascriptExecutor)this;
			es.forEach(e -> jsExecutor.executeScript("arguments[0].parentNode.removeChild(arguments[0])", e));
		} catch (Exception e) {
		}
	}

	public void setText(By by, String text, int timeout) {
		try {
			WebElement e = findElement(by, timeout);
			if (e == null) {
				return;
			}

			JavascriptExecutor jsExecutor = (JavascriptExecutor)this;
			jsExecutor.executeScript("arguments[0].innerHTML = \"" + text + "\"", e);
		} catch (Exception e) {
		}
	}

	public List<WebElement> findElements(By xpath, String mark, int milli) throws Exception {
		Exception previous = null;
		while (milli >= 0) {
			try {
				List<WebElement> es = super.findElements(xpath);
				String text = toString(es);
				if (!mark.contentEquals(text)) {
					return es;
				}
			} catch (Exception e) {
				previous = e;
			}
			Utility.sleep(PAUSE);
			milli -= PAUSE;
		}
		throw previous;
	}

	public boolean isEmpty(By xpath) {
		try {
			List<WebElement> es = super.findElements(xpath);
			return es.isEmpty();
		} catch (Exception e) {
		}
		return true;
	}

	public boolean isEmpty(By xpath, int milli) {
		while (milli >= 0) {
			try {
				List<WebElement> es = super.findElements(xpath);
				return es.isEmpty();
			} catch (Exception e) {
			}
			Utility.sleep(PAUSE);
			milli -= PAUSE;
		}
		return true;
	}

	public boolean clickIfExist(By xpath) {
		try {
			WebElement e = super.findElement(xpath);
			e.click();
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public String toString(List<WebElement> es) {
		StringBuffer buffer = new StringBuffer();
		es.forEach(e -> buffer.append(e.getText()));
		return new String(buffer);
	}

	public String extractTextContentFromTrElement(WebElement tr) {
		return extractTextContentFromTrElement(tr, "");
	}

	private static String extractTextContentFromTrElement(WebElement tr, String prefix) {
		StringBuffer sb = new StringBuffer(prefix);
		tr.findElements(By.tagName("th")).forEach(th -> {
			sb.append(th.getAttribute("textContent"));
			sb.append("\t");
		});
		tr.findElements(By.tagName("td")).forEach(td -> {
			sb.append(td.getAttribute("textContent"));
			sb.append("\t");
		});
		sb.append("\n");
		return new String(sb);
	}

	public String extractTextContentFromTableElement(WebElement e) {
		return extractTextContentFromTableElement(e, "");
	}

	public String extractTextContentFromTableElement(WebElement e, String prefix) {
		log.info("{} extractTextFromTableElement(..., 『{}』)", Utility.indentStart(), Utility.ellipsisEscape(prefix, 16));
		long started = System.currentTimeMillis();

		StringBuffer sb = new StringBuffer();
		e.findElements(By.tagName("tr")).forEach(tr -> sb.append(extractTextContentFromTrElement(tr, prefix)));
		String result = new String(sb);

		log.info("{} {} extractTextFromTableElement(..., 『{}』) - {}", Utility.indentEnd(), Utility.ellipsisEscape(result, 32), Utility.ellipsisEscape(prefix, 16), Utility.toStringPastTimeReadable(started));
		return result;
	}

	public String extractTextFromTableElement(WebElement e) {
		return extractTextFromTableElement(e, "");
	}

	public String extractTextFromTableElement(WebElement e, String prefix) {
		log.info("{} extractTextFromTableElement(..., 『{}』)", Utility.indentStart(), Utility.ellipsisEscape(prefix, 16));
		long started = System.currentTimeMillis();

		StringBuffer sb = new StringBuffer();
		e.findElements(By.tagName("tr")).forEach(tr -> sb.append(extractTextFromTrElement(tr, prefix)));
		String result = new String(sb);

		log.info("{} {} extractTextFromTableElement(..., 『{}』) - {}", Utility.indentEnd(), Utility.ellipsisEscape(result, 32), Utility.ellipsisEscape(prefix, 16), Utility.toStringPastTimeReadable(started));
		return result;
	}

	private String extractTextFromTrElement(WebElement tr, String prefix) {
		StringBuffer sb = new StringBuffer(prefix);
		tr.findElements(By.tagName("th")).forEach(th -> {
			sb.append(th.getText().replaceAll("[\n\t]+", " "));
			sb.append("\t");
		});
		tr.findElements(By.tagName("td")).forEach(td -> {
			sb.append(td.getText().replaceAll("[\n\t]+", " "));
			sb.append("\t");
		});
		sb.append("\n");
		return new String(sb);
	}

	public String getText(By by) {
		try {
			WebElement element = super.findElement(by);
			return element.getText();
		} catch (Exception e) {
		}
		return "NaN";
	}
	public String getText(By by, Duration duration) {
		String result = "NaN";

		try {
			Duration durationPrevious = manage().timeouts().getImplicitWaitTimeout();
			manage().timeouts().implicitlyWait(duration);

			try {
				WebElement element = super.findElement(by);
				result = element.getText();
			} catch (Exception e) {
			}

			manage().timeouts().implicitlyWait(durationPrevious);
		} catch (Exception e) {
		}

		return result;
	}

	public String getText(By by, String defaultValue) {
		try {
			WebElement element = super.findElement(by);
			return element.getText();
		} catch (Exception e) {
		}
		return defaultValue;
	}

	public String getText(By xpath, int milli, String defaultValue) {
		long end = System.currentTimeMillis() + milli;
		List<WebElement> elements = null;
		do {
			try {
				elements = super.findElements(xpath);
				if (!elements.isEmpty()) {
					return elements.get(elements.size() - 1).getText();
				}
			} catch (Exception e) {
			}
			Utility.sleep(PAUSE);
		} while (System.currentTimeMillis() < end);
		return defaultValue;
	}

	public String getTextLast(By xpath, int milli, String value) {
		try {
			List<WebElement> es = findElements(xpath, milli);
			if (es != null && es.size() > 0) {
				WebElement e = es.get(es.size() - 1);
				return e.getText();
			}
		} catch (Exception e) {
		}

		return value;
	}

	public String getTextFromTableElement(WebElement table, String prefix) {
		String lines = table.getText();
		StringBuffer sb = new StringBuffer();
		for (String line : lines.split("\r?\n")) {
			sb.append(prefix);
			sb.append(line);
			sb.append("\n");
		}
		return sb.toString();
	}

	public boolean waitUntilIsDisplayed(By xpath, boolean b, int milli) {
		while (milli >= 0) {
			try {
				WebElement e = super.findElement(xpath);
				if (e.isDisplayed() == b) {
					return true;
				}
			} catch (Exception e) {
			}
			Utility.sleep(PAUSE);
			milli -= PAUSE;
		}
		return false;
	}

	public WebElement lastElement(By xpath, int timeout) {
		try {
			List<WebElement> es = findElements(xpath, timeout);
			if (es == null || es.isEmpty()) {
				return null;
			}

			return es.get(es.size() - 1);
		} catch (Exception e) {
		}
		return null;
	}

	public boolean match(By by, String pattern, Duration duration) {
		log.trace("{} match(..., 『{}』)", Utility.indentStart(), pattern);
		long started = System.currentTimeMillis();

		try {
			boolean result = Pattern.compile(pattern).matcher(getText(by, duration)).find();

			log.trace("{} 『{}』 match(..., 『{}』) - {}", Utility.indentEnd(), result, pattern, Utility.toStringPastTimeReadable(started));
			return true;
		} catch (Exception e) {
		}

		log.trace("{} {} match(..., 『{}』) - {}", Utility.indentEnd(), false, pattern, Utility.toStringPastTimeReadable(started));
		return false;
	}

	public boolean waitUntilExist(By xpath, boolean b, int milli) {
		while (milli >= 0) {
			try {
				super.findElement(xpath);
				if (b) {
					return true;
				}
			} catch (Exception e) {
				if (!b) {
					return true;
				}
			}
			Utility.sleep(PAUSE);
			milli -= PAUSE;
		}
		return false;
	}

	public boolean waitUntilNotIncludeTextLast(By xpath, int milli, String previous) {
		while (milli >= 0) {
			try {
				String current = getTextLast(xpath, 1, previous);
				if (!current.contentEquals(previous)) {
					return true;
				}
			} catch (Exception e) {
			}
			Utility.sleep(PAUSE);
			milli -= PAUSE;
		}
		return false;
	}

	public String getText(List<WebElement> result) {
		StringBuffer sb = new StringBuffer();
		for (WebElement e : result) {
			sb.append(e.getText());
		}
		return sb.toString();
	}

	public String getText(WebElement element, By by) {
		try {
			WebElement result = element.findElement(by);
			return result.getText();
		} catch (Exception e) {
		}
		return "NaN";
	}
	public String getText(WebElement element, By by, Duration duration) {
		String result = "NaN";
		try {
			Duration durationPrevious = manage().timeouts().getImplicitWaitTimeout();
			manage().timeouts().implicitlyWait(duration);

			try {
				result = element.findElement(by).getText();
			} catch (Exception e) {
			}

			manage().timeouts().implicitlyWait(durationPrevious);
		} catch (Exception e) {
		}

		return result;
	}

	public String getAttribute(WebElement e, String attributeName, String prefix) {
		String lines = e.getAttribute(attributeName);
		StringBuffer sb = new StringBuffer();
		for (String line : lines.split("\r?\n")) {
			sb.append(prefix);
			sb.append(line);
			sb.append("\n");
		}
		return sb.toString();
	}

	public String getAttributeLast(By xpath, String attributeName, int milli, String value) {
		try {
			List<WebElement> es = findElements(xpath, milli);
			if (es != null && es.size() > 0) {
				WebElement e = es.get(es.size() - 1);
				return e.getAttribute(attributeName);
			}
		} catch (Exception e) {
		}

		return value;
	}

	public List<WebElement> findElements(WebElement element, By by) {
		try {
			return element.findElements(by);
		} catch (Exception e) {
		}

		return null;
	}

	public boolean visibilityOf(WebElement element) {
		try {
			wait.until(ExpectedConditions.visibilityOf(element));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public boolean visibilityOfElementLocated(By by) {
		try {
			wait.until(ExpectedConditions.visibilityOfElementLocated(by));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public boolean elementToBeClickable(By by) {
		try {
			wait.until(ExpectedConditions.elementToBeClickable(by));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public boolean invisibilityOfElementLocated(By by) {
		try {
			wait.until(ExpectedConditions.invisibilityOfElementLocated(by));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public boolean invisibilityOfElementLocated(By by, Duration duration) {
		try {
			WebDriverWait wait = new WebDriverWait(this, duration);
			wait.until(ExpectedConditions.invisibilityOfElementLocated(by));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public boolean frameToBeAvailableAndSwitchToIt(String frame, Duration duration) {
		try {
			WebDriverWait wait = new WebDriverWait(this, duration);
			wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frame));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}
	public boolean frameToBeAvailableAndSwitchToIt(String frame, Duration duration, boolean showException) {
		try {
			WebDriverWait wait = new WebDriverWait(this, duration);
			wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frame));
			return true;
		} catch (Exception e) {
			if (showException) {
				log.warn("Exception:: {}", e.getLocalizedMessage(), e);
			}
		}
		return false;
	}

	public boolean visibilityOfElementLocated(By by, Duration duration) {
		for (;;) {
			try {
				WebDriverWait wait = new WebDriverWait(this, duration);
				wait.until(ExpectedConditions.visibilityOfElementLocated(by));
				return true;
			} catch (UnhandledAlertException e) {
			} catch (Exception e) {
				log.warn("Exception:: {}", e.getLocalizedMessage(), e);
				break;
			}
		}
		return false;
	}

	public boolean frameToBeAvailableAndSwitchToIt(String frame) {
		try {
			wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frame));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public boolean elementToBeClickable(By by, Duration duration) {
		try {
			WebDriverWait wait = new WebDriverWait(this, duration);
			wait.until(ExpectedConditions.elementToBeClickable(by));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public boolean presenceOfElementLocated(By by) {
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(by));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public boolean presenceOfElementLocated(By by, Duration duration) {
		try {
			WebDriverWait wait = new WebDriverWait(this, duration);
			wait.until(ExpectedConditions.presenceOfElementLocated(by));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public void notPresenceOfAllElementsLocatedBy(By by, int milli) {
		long end = System.currentTimeMillis() + milli;
		do {
			try {
				wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(by));
				wait(PAUSE);
			} catch (Exception e) {
			}
		} while (System.currentTimeMillis() < end);
	}

	public boolean mouseHover(By by) {
		try {
			WebElement ele = findElement(by);
			Actions action = new Actions(this);
			action.moveToElement(ele).perform();
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public boolean mouseClick(By by) {
		try {
			WebElement ele = findElement(by);
			Actions actions = new Actions(this);
			actions.moveToElement(ele).perform();
			actions.click().build().perform();
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public boolean scrollTo(By by) {
		try {
			WebElement element = findElement(by);
			((JavascriptExecutor)this).executeScript("arguments[0].scrollIntoView(true);", element);
			Actions actions = new Actions(this);
			actions.moveToElement(element);
			actions.perform();
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}

	public void sendKeys(By by, String path) {
		try {
			WebElement element = super.findElement(by);
			element.sendKeys(path);
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}
	}

	public boolean numberOfElementsToBeMoreThan(By by, int i) {
		try {
			wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(by, i));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return false;
	}
	public boolean numberOfElementsToBeMoreThan(By by, int size, Duration duration) {
		try {
			WebDriverWait wait = new WebDriverWait(this, duration);
			wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(by, size));
			return true;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}

		return false;
	}

	public void refresh() {
		Window window = manage().window();
		Dimension size = window.getSize();
		window.setSize(new Dimension(size.getWidth() / 2, size.getHeight() / 2));
		window.setSize(size);
		
		String url = this.getCurrentUrl();
		this.get(url);
		this.visibilityOfElementLocated(By.tagName("body"));
	}

	public boolean isClickable(By by) {
		try {
			WebElement element = findElement(by);
			return element.isDisplayed();
		} catch (Exception e) {
		}
		return false;
	}

	public boolean isDisplayed(By by) {
		try {
			WebElement element = findElement(by);
			return element.isDisplayed();
		} catch (Exception e) {
		}
		return false;
	}

	public boolean isDisplayed(By by, Duration duration) {
		try {
			WebElement element = findElement(by, duration);
			return element.isDisplayed();
		} catch (Exception e) {
		}
		return false;
	}

	public boolean isDisplayed(WebElement element, By by) {
		try {
			WebElement result = element.findElement(by);
			return result.isDisplayed();
		} catch (Exception e) {
		}
		return false;
	}
	public boolean isDisplayed(WebElement element, By by, Duration duration) {
		boolean result = false;
		try {
			Duration durationPrevious = manage().timeouts().getImplicitWaitTimeout();
			manage().timeouts().implicitlyWait(duration);

			try {
				result = element.findElement(by).isDisplayed();
			} catch (Exception e) {
			}

			manage().timeouts().implicitlyWait(durationPrevious);
		} catch (Exception e) {
		}

		return result;
	}

	public String innerHtml(By by) {
		try {
			WebElement element = findElement(by);
			String contents = (String)((JavascriptExecutor)this).executeScript("return arguments[0].innerHTML;", element);
			return contents;
		} catch (Exception e) {
			log.warn("Exception:: {}", e.getLocalizedMessage(), e);
		}
		return "";
	}

}
