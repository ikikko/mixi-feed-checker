package org.ikikko;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URLEncodedUtils;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * mixi Graph API を通じて、マイミクの更新情報を取得するクラスです。
 *
 * @author ikikko
 * @see http://developer.mixi.co.jp/connect/mixi_graph_api/api_auth
 * @see http://developer.mixi.co.jp/connect/mixi_graph_api/mixi_io_spec_top/updates-api
 *
 */
public class MixiFeedChecker {

	private static final String AUTHORIZATION_URL = "https://mixi.jp/connect_authorize.pl";
	private static final String ACCESS_TOKEN_URL = "https://secure.mixi-platform.com/2/token";
	private static final String UPDATES_URL = "http://api.mixi-platform.com/2/updates/@me/@friends";

	Writer writer;

	WebClient client;

	Properties properties;

	HtmlPage currentPage;

	String authorizationCode;

	MixiGraphApiToken token;

	String feed;

	/**
	 * マイミク更新情報取得クラスを構築します。
	 *
	 * <p>
	 * デフォルトでは {@link StringWriter} に出力します。
	 * </p>
	 */
	public MixiFeedChecker() throws IOException {
		this(new StringWriter());
	}

	/**
	 * マイミク更新情報取得クラスを構築します。
	 */
	public MixiFeedChecker(Writer writer) throws IOException {
		this.writer = writer;
		this.client = new WebClient();
		this.properties = new Properties();

		InputStream in = null;
		try {
			in = this.getClass().getResourceAsStream(
					"/mixi-graph-api.properties");
			properties.load(in);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	/**
	 * マイミク更新を実行します。
	 *
	 * <p>
	 * ログイン～認可～アクセストークン取得～API発行までを順次行い、{@link #writer} にフィード内容を書き込みます。
	 * </p>
	 */
	public void execute() throws IOException, URISyntaxException {
		start();
		login();
		authorize();
		fetchToken();
		fetchUpdates();

		write();
	}

	/**
	 * 処理を開始します。
	 *
	 * <p>
	 * メソッド呼び出し後、{@link #currentPage} には最初のページ（ログインページ）がセットされます。
	 * </p>
	 */
	void start() throws IOException {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair("client_id", properties
				.getProperty("CONSUMER_KEY")));
		params.add(new NameValuePair("response_type", "code"));
		params.add(new NameValuePair("scope", "r_updates"));
		params.add(new NameValuePair("display", "pc"));

		WebRequest request = new WebRequest(new URL(AUTHORIZATION_URL));
		request.setRequestParameters(params);

		currentPage = client.getPage(request);
	}

	/**
	 * ログインします。
	 *
	 * <p>
	 * メソッド呼び出し後、{@link #currentPage} にはサービス認可用のページがセットされます。
	 * </p>
	 */
	void login() throws IOException {
		HtmlForm form = currentPage.getFormByName("login_form");
		form.getInputByName("email").setValueAttribute(
				properties.getProperty("EMAIL"));
		form.getInputByName("password").setValueAttribute(
				properties.getProperty("PASSWORD"));

		// ログインボタンにはname/idが割り当てられていないので、親ノードから参照する
		HtmlElement buttonParagraph = form.getElementsByAttribute("p", "class",
				"loginButton").get(0);
		HtmlElement button = (HtmlElement) buttonParagraph.getFirstChild();

		currentPage = button.click();
	}

	/**
	 * mixi Graph APIのサービスを認可します。
	 *
	 * <p>
	 * メソッド呼び出し後、{@link #authorizationCode} に Authorization Code がセットされます。
	 * </p>
	 *
	 * @return Authorization Code
	 * @see http://developer.mixi.co.jp/connect/mixi_graph_api/api_auth#toc-
	 *      authorization-code
	 */
	String authorize() throws IOException, URISyntaxException {
		HtmlForm form = currentPage.getForms().get(0);

		currentPage = form.getInputByName("accept").click();

		List<org.apache.http.NameValuePair> params = URLEncodedUtils.parse(
				currentPage.getUrl().toURI(), "UTF-8");
		for (org.apache.http.NameValuePair param : params) {
			if (param.getName().equals("code")) {
				authorizationCode = param.getValue();
				return authorizationCode;
			}
		}

		throw new IllegalStateException("Authorization Code が取得できませんでした");
	}

	/**
	 * トークンを取得します。
	 *
	 * <p>
	 * メソッド呼び出し後、{@link #token}
	 * にはリフレッシュトークン・アクセストークン・アクセストークンが失効するまでの期間がセットされます。
	 * </p>
	 *
	 * @return トークン
	 */
	MixiGraphApiToken fetchToken() throws IOException {

		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair("grant_type", "authorization_code"));
		params.add(new NameValuePair("client_id", properties
				.getProperty("CONSUMER_KEY")));
		params.add(new NameValuePair("client_secret", properties
				.getProperty("CONSUMER_SECRET")));
		params.add(new NameValuePair("code", authorizationCode));
		params.add(new NameValuePair("redirect_uri", properties
				.getProperty("REDIRECT_URL")));

		WebRequest request = new WebRequest(new URL(ACCESS_TOKEN_URL));
		request.setHttpMethod(HttpMethod.POST);
		request.setRequestParameters(params);

		WebResponse response = client.getPage(request).getWebResponse();

		Gson gson = new GsonBuilder().setFieldNamingPolicy(
				FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
		token = gson.fromJson(response.getContentAsString(),
				MixiGraphApiToken.class);

		return token;
	}

	/**
	 * マイミク更新情報を取得します。
	 *
	 * <p>
	 * メソッド呼び出し後、{@link #feed} にはマイミク更新情報がセットされます。
	 * </p>
	 *
	 * @return マイミク更新情報
	 */
	String fetchUpdates() throws IOException {

		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair("format", "atom"));
		params.add(new NameValuePair("fields", "diary"));
		params.add(new NameValuePair("count", "100"));

		WebRequest request = new WebRequest(new URL(UPDATES_URL));
		request.setAdditionalHeader("Authorization",
				"OAuth " + token.getAccessToken());
		request.setRequestParameters(params);

		WebResponse response = client.getPage(request).getWebResponse();
		feed = response.getContentAsString();

		return feed;
	}

	/**
	 * マイミク更新情報を書き込みます。
	 */
	void write() throws IOException {
		writer.write(feed);
	}

}