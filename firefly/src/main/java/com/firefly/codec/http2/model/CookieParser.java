package com.firefly.codec.http2.model;

import java.util.ArrayList;
import java.util.List;

import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;

abstract public class CookieParser {

	public interface CookieParserCallback {
		public void cookie(String name, String value);
	}

	public static void parseCookies(String cookieStr, CookieParserCallback callback) {
		if (VerifyUtils.isEmpty(cookieStr)) {
			throw new IllegalArgumentException("the cookie string is empty");
		} else {
			String[] cookieKeyValues = StringUtils.split(cookieStr, ';');
			for (String cookieKeyValue : cookieKeyValues) {
				String[] kv = StringUtils.split(cookieKeyValue, "=", 2);
				if (kv != null) {
					if (kv.length == 2) {
						callback.cookie(kv[0].trim(), kv[1].trim());
					} else if (kv.length == 1) {
						callback.cookie(kv[0].trim(), "");
					} else {
						throw new IllegalStateException("the cookie string format error");
					}
				} else {
					throw new IllegalStateException("the cookie string format error");
				}
			}
		}
	}

	public static Cookie parseSetCookie(String cookieStr) {
		final Cookie cookie = new Cookie();
		parseCookies(cookieStr, new CookieParserCallback() {

			@Override
			public void cookie(String name, String value) {
				if ("Comment".equalsIgnoreCase(name)) {
					cookie.setComment(value);
				} else if ("Domain".equalsIgnoreCase(name)) {
					cookie.setDomain(value);
				} else if ("Max-Age".equalsIgnoreCase(name)) {
					cookie.setMaxAge(Integer.parseInt(value));
				} else if ("Path".equalsIgnoreCase(name)) {
					cookie.setPath(value);
				} else if ("Secure".equalsIgnoreCase(name)) {
					cookie.setSecure(true);
				} else if ("Version".equalsIgnoreCase(name)) {
					cookie.setVersion(Integer.parseInt(value));
				} else {
					cookie.setName(name);
					cookie.setValue(value);
				}

			}
		});
		return cookie;
	}

	public static List<Cookie> parseCookie(String cookieStr) {
		final List<Cookie> list = new ArrayList<>();
		parseCookies(cookieStr, new CookieParserCallback() {

			@Override
			public void cookie(String name, String value) {
				list.add(new Cookie(name, value));
			}
		});
		return list;
	}

	public static List<javax.servlet.http.Cookie> parserServletCookie(String cookieStr) {
		final List<javax.servlet.http.Cookie> list = new ArrayList<>();
		parseCookies(cookieStr, new CookieParserCallback() {

			@Override
			public void cookie(String name, String value) {
				list.add(new javax.servlet.http.Cookie(name, value));
			}
		});
		return list;
	}
}
