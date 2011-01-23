package org.ikikko;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

import java.io.StringReader;

import org.junit.Test;

import com.sun.syndication.io.SyndFeedInput;

public class MixiFeedCheckerTest {

	// 本来なら各メソッドごとにテストするべきだけど、
	// ステートレスな処理かつ一つ一つの処理に結構時間がかかるので
	// まとめてテスト実行

	@Test
	public void execute() throws Exception {
		MixiFeedChecker checker = new MixiFeedChecker();
		assertThat(checker.currentPage, is(nullValue()));

		// 開始
		checker.start();
		assertThat(checker.currentPage.asText(), containsString("ログイン"));

		// ログイン
		checker.login();
		assertThat(checker.currentPage.asText(), containsString("許可を要求"));

		// 認可
		String authorizationCode = checker.authorize();
		assertThat(authorizationCode, is(notNullValue()));
		System.out.println("[Authorization Code] " + checker.authorizationCode);

		// アクセストークン取得
		MixiGraphApiToken token = checker.fetchToken();
		assertThat(token.getRefreshToken(), is(notNullValue()));
		assertThat(token.getExpiresIn(), is(notNullValue()));
		assertThat(token.getAccessToken(), is(notNullValue()));
		System.out.println("[Access Token] " + token.getAccessToken());

		// API発行
		String feed = checker.fetchUpdates();
		SyndFeedInput input = new SyndFeedInput();
		assertThat(input.build(new StringReader(feed)), is(notNullValue()));

		// フィード書き込み
		checker.write();
		System.out.println("[Feed]");
		System.out.println(checker.writer);
	}

}
