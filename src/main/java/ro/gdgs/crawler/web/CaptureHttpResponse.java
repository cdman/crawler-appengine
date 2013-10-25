package ro.gdgs.crawler.web;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

final class CaptureHttpResponse extends HttpServletResponseWrapper {
	private final StringWriter writer;

	public CaptureHttpResponse(HttpServletResponse response) {
		super(response);
		this.writer = new StringWriter();
	}

	@Override
	public PrintWriter getWriter() {
		return new PrintWriter(writer);
	}
	
	StringWriter getStringWriter() {
		return writer;
	}
}
