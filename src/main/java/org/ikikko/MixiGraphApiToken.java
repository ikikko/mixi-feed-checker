package org.ikikko;

public final class MixiGraphApiToken {

	/** リフレッシュトークン */
	private String refreshToken;

	/** アクセストークンの有効期限 */
	private String expiresIn;

	/** アクセストークン */
	private String accessToken;

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(String expiresIn) {
		this.expiresIn = expiresIn;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	@Override
	public String toString() {
		return "MixiGraphApiToken [refresh_token=" + refreshToken
				+ ", expires_in=" + expiresIn + ", access_token=" + accessToken
				+ "]";
	}

}
