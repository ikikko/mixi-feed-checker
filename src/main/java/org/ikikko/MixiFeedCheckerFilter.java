package org.ikikko;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class MixiFeedCheckerFilter implements Filter {

	@Override
	public void init(FilterConfig filterconfig) throws ServletException {
		// do noting...
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {

		Properties properties = new Properties();
		InputStream in = null;
		try {
			in = this.getClass().getResourceAsStream("/filter.properties");
			properties.load(in);
		} finally {
			IOUtils.closeQuietly(in);
		}

		String propertyToken = properties.getProperty("token");
		String parameterToken = req.getParameter("token");

		if (isAuthentiated(propertyToken, parameterToken)) {
			chain.doFilter(req, resp);
		} else {
			((HttpServletResponse) resp)
					.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}

	}

	boolean isAuthentiated(String propertyToken, String parameterToken) {
		if (StringUtils.isEmpty(propertyToken)) {
			// プロパティファイルにトークンが定義されてない場合
			return true;

		} else if (StringUtils.equals(propertyToken, parameterToken)) {
			// プロパティファイルにトークンが定義されていて、クエリパラメータと一致する場合
			return true;

		} else {
			// それ以外（プロパティファイルにトークンが定義されていて、クエリパラメータと一致しない場合)
			return false;
		}
	}

	@Override
	public void destroy() {
		// do noting...
	}

}
