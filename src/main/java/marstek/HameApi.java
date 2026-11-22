package marstek;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.StringUtil;

/**
 * Minimal client for the Marstek/Hame cloud API (eu.hamedata.com), used to
 * resolve the salt based cloud topic id of a device. Port of hame-relay
 * src/hame_api.ts (login + device list) and src/topic.ts (cq algorithm).
 */
public class HameApi
{
	private static final String	BASE_URL	= "https://eu.hamedata.com";
	private static final String	CHARSET		= "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	/**
	 * Logs in to the cloud API, looks up the device by MAC and computes its salt
	 * based topic id. Returns null if the account/device has no usable salt
	 * (the caller should fall back to the encrypted MAC id).
	 */
	public static String resolveCloudDeviceId(final String mailbox, final String password, final String mac) throws Exception
	{
		final String token = fetchToken(mailbox, password);
		final String list = httpGet("/ems/api/v1/getDeviceList?mailbox=" + urlEncode(mailbox) + "&token=" + urlEncode(token));

		// find the device entry matching our MAC (normalize: lowercase, hex only)
		final String macNorm = normalizeMac(mac);
		final Matcher objects = Pattern.compile("\\{[^{}]*\\}").matcher(list);
		while (objects.find())
		{
			final String obj = objects.group();
			final String objMac = jsonField(obj, "mac");
			if (objMac == null || !normalizeMac(objMac).equals(macNorm))
				continue;

			final String type = jsonField(obj, "type");
			final String saltPair = jsonField(obj, "salt");
			if (type == null)
				return null;
			return resolveTopicId(saltPair, mac, type);
		}
		return null; // device not in this account
	}

	// --- API calls ---------------------------------------------------------

	private static String fetchToken(final String mailbox, final String password) throws Exception
	{
		final String md5hex = toHex(MessageDigest.getInstance("MD5").digest(password.getBytes(StandardCharsets.UTF_8)));
		final String response = httpGet("/app/Solar/v2_get_device.php?mailbox=" + urlEncode(mailbox) + "&pwd=" + md5hex);

		if (response.matches(".*\"code\"\\s*:\\s*\"?4\"?.*"))
			throw new IOException("Cloud API rejected the credentials (wrong password)");
		final String token = jsonField(response, "token");
		if (token == null)
			throw new IOException("Cloud API login failed: " + StringUtil.abbreviate(response, 120));
		return token;
	}

	private static String httpGet(final String pathAndQuery) throws Exception
	{
		final HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(BASE_URL + pathAndQuery))
			.header("User-Agent", "Dart/2.19 (dart:io)")
			.GET()
			.build();
		final HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200)
			throw new IOException("Cloud API HTTP " + response.statusCode());
		return response.body();
	}

	// --- topic id resolution (hame-relay CommonHelper.resolveTopicId) ------

	/**
	 * salt pair is "<current>,<issued>". "0" in the first slot means no id in
	 * use yet; unusable pairs fall back to the topic encryption id on the
	 * caller's side. In both usable states the device answers on the id
	 * computed from the current salt.
	 */
	static String resolveTopicId(final String saltPair, final String mac, final String vid) throws Exception
	{
		if (saltPair == null || saltPair.isEmpty())
			return null;
		final String[] parts = saltPair.split(",", -1);
		if (parts.length < 2)
			return null;
		final String current = parts[0];
		final String issued = parts[1];
		if (current.equals("0") || current.isEmpty() || issued.isEmpty() || issued.equals("0"))
			return null;
		return cq(current, mac, vid);
	}

	// --- cq algorithm (hame-relay src/topic.ts) ----------------------------

	public static String cq(final String salt, final String mac, final String vid) throws Exception
	{
		if (mac.length() < 4)
			return "";

		final String var1 = vid + "_" + mac.substring(0, mac.length() - 4);
		final String var2 = mac.substring(1, mac.length() - 2) + "_" + vid;

		final String h1 = textForRand(salt, var1);
		final String h2 = streamE(vid + mac, var2);

		return codeE(h1 + h2).substring(0, 24);
	}

	/** HexUtil.textForRand: scrambles the salt bytes N times with a key derived permutation. */
	private static String textForRand(final String content, final String macKey)
	{
		if (content == null || content.isEmpty() || macKey == null || macKey.isEmpty())
			return "";
		final String hexContent = toHex(content.getBytes(StandardCharsets.UTF_8));
		if (hexContent.length() < 2)
			return "";

		int n = Integer.parseInt(hexContent.substring(hexContent.length() - 2), 16) % 5;
		if (n == 0)
			n = 1;

		byte[] bytes = fromHex(hexContent);
		final byte[] keyBytes = fromHex(toHex(macKey.getBytes(StandardCharsets.UTF_8)));
		for (int i = 0; i < n; i++)
			bytes = scramble(bytes, keyBytes);
		return toHex(bytes);
	}

	private static byte[] scramble(final byte[] data, final byte[] key)
	{
		if (data.length == 0)
			return data;
		final int[] p = buildPermutation(key, data.length);
		final byte[] result = new byte[data.length];
		for (int i = 0; i < data.length; i++)
			result[i] = data[p[i]];
		return result;
	}

	/** RC4-KSA style permutation table. */
	private static int[] buildPermutation(final byte[] key, final int size)
	{
		final int[] p = new int[size];
		for (int i = 0; i < size; i++)
			p[i] = i;
		if (size <= 1)
			return p;

		int j = 0;
		for (int i = 0; i < size; i++)
		{
			j = (j + p[i] + (key[i % key.length] & 0xff)) % size;
			final int t = p[i];
			p[i] = p[j];
			p[j] = t;
		}
		return p;
	}

	/** StreamUtil.e: XOR the data with an LCG keystream seeded from the key, hex encoded. */
	private static String streamE(final String data, final String key)
	{
		final byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
		long seed = 0;
		for (final byte b : keyBytes)
			seed = (seed * 31 + (b & 0xff)) % 2147483647L;

		final byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
		final long[] keyStream = new long[data.length()]; // length = char count of data
		long state = seed;
		for (int i = 0; i < keyStream.length; i++)
		{
			state = (state * 1664525L + 1013904223L) % 4294967296L;
			keyStream[i] = (state ^ (state >>> 16)) & 0xff;
		}

		final StringBuilder hex = new StringBuilder(dataBytes.length * 2);
		for (int i = 0; i < dataBytes.length; i++)
		{
			final int out = (dataBytes[i] & 0xff) ^ (int) keyStream[i % keyStream.length];
			hex.append(String.format("%02x", out));
		}
		return hex.toString();
	}

	/** CodeUtil.e: SHA-256 based custom encoding (48 chars for 24 extracted bytes). */
	private static String codeE(final String input) throws Exception
	{
		final byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
		final long[] words = new long[8];
		for (int i = 0; i < 8; i++)
		{
			long w = 0;
			for (int b = 0; b < 4; b++)
				w = (w << 8) | (digest[i * 4 + b] & 0xff);
			words[i] = w;
		}

		final StringBuilder sb = new StringBuilder(48);
		for (int i = 0; i < 24; i++)
		{
			final long word = words[i % 8];
			final int shift = (i / 8) * 8;
			final int b = (int) ((word >>> shift) & 0xff);
			sb.append(CHARSET.charAt(b % 62)).append(CHARSET.charAt((b * 31) % 62));
		}
		return sb.toString();
	}

	// --- helpers -----------------------------------------------------------

	private static String normalizeMac(final String mac)
	{
		return mac.toLowerCase().replaceAll("[^0-9a-f]", "");
	}

	private static String jsonField(final String json, final String field)
	{
		final Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
		return m.find() ? m.group(1) : null;
	}

	private static String urlEncode(final String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String toHex(final byte[] bytes)
	{
		final StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (final byte b : bytes)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	private static byte[] fromHex(final String hex)
	{
		final byte[] out = new byte[hex.length() / 2];
		for (int i = 0; i < out.length; i++)
			out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		return out;
	}
}
