package com.erumpay.pg_auth_service.security;

import com.erumpay.pg_auth_service.exception.AuthErrorCode;
import com.erumpay.pg_auth_service.exception.AuthException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

	private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
	private static final int TIME_STEP_SECONDS = 30;
	private static final int CODE_DIGITS = 6;
	private static final int ALLOWED_WINDOW = 1;

	public boolean verify(String secret, String code) {
		if (secret == null || secret.isBlank() || code == null || !code.matches("\\d{6}")) {
			return false;
		}

		long counter = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
		for (int offset = -ALLOWED_WINDOW; offset <= ALLOWED_WINDOW; offset++) {
			if (generateCode(secret, counter + offset).equals(code)) {
				return true;
			}
		}
		return false;
	}

	private String generateCode(String secret, long counter) {
		try {
			byte[] key = decodeBase32(secret);
			byte[] data = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key, "HmacSHA1"));
			byte[] hash = mac.doFinal(data);

			int offset = hash[hash.length - 1] & 0x0f;
			int binary = ((hash[offset] & 0x7f) << 24)
				| ((hash[offset + 1] & 0xff) << 16)
				| ((hash[offset + 2] & 0xff) << 8)
				| (hash[offset + 3] & 0xff);
			int otp = binary % (int) Math.pow(10, CODE_DIGITS);
			return String.format("%06d", otp);
		} catch (Exception ex) {
			throw new AuthException(AuthErrorCode.ADMIN_TOTP_VERIFICATION_FAILED, ex);
		}
	}

	private byte[] decodeBase32(String value) {
		String normalized = value.replace("=", "")
			.replace(" ", "")
			.toUpperCase(Locale.ROOT);
		ByteBuffer buffer = ByteBuffer.allocate(normalized.length() * 5 / 8);

		int bits = 0;
		int bitBuffer = 0;
		for (char c : normalized.toCharArray()) {
			int index = BASE32_ALPHABET.indexOf(c);
			if (index < 0) {
				throw new IllegalArgumentException("Invalid base32 character");
			}
			bitBuffer = (bitBuffer << 5) | index;
			bits += 5;
			if (bits >= 8) {
				buffer.put((byte) ((bitBuffer >> (bits - 8)) & 0xff));
				bits -= 8;
			}
		}

		byte[] decoded = new byte[buffer.position()];
		buffer.flip();
		buffer.get(decoded);
		return decoded;
	}
}
