package unimelb.bitbox.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import unimelb.bitbox.util.Configuration;



public class Crypto {
	
	private static final String PKCS_1_PEM_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
	private static final String PKCS_1_PEM_FOOTER = "-----END RSA PRIVATE KEY-----";

	public Crypto() {
		
	}
	
	/*TESTING
	public static void main(String[] args) throws Exception {

		String[] authorizedKeys = Configuration.getConfigurationValue("authorized_keys").split("\\s*,\\s*");
		sortKeys(authorizedKeys);
		String identity = "emanmacario@Allan-PC";	



		PublicKey publicKey = loadPublicKey(identity);
		String secretKey = loadSecretKey();
		PrivateKey privateKey = loadPrivateKey("bitboxclient_rsa");

		//Key Pair Test
		String cipherText = encrypt(secretKey, publicKey);
		String decipheredMessage = decrypt(cipherText, privateKey);

		//AES Test
		String msg = "walid";
		System.out.println(msg);
		String cipherTextAES = encryptAES(msg, secretKey);
		System.out.println(cipherTextAES);
		String decipheredMessageAES = decryptAES(cipherTextAES, secretKey);
		System.out.println(decipheredMessageAES);
	}
*/
	
	
	public static String encryptAES(String msg, String secretKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
		Key aesKey = new SecretKeySpec(secretKey.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, aesKey);
		byte[] encryptedAES = cipher.doFinal(msg.getBytes("UTF-8"));
		return new String(Base64.getEncoder().encodeToString(encryptedAES));
	}
	
	public static String decryptAES(String cipherTextAES, String secretKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		 
		Key aesKey = new SecretKeySpec(secretKey.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, aesKey);
		cipherTextAES = new String(cipher.doFinal(Base64.getDecoder().decode(cipherTextAES.getBytes())));
		return cipherTextAES;
	}


	public static String loadSecretKey() throws NoSuchAlgorithmException {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128);
		SecretKey secretKey = kgen.generateKey();
		String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
		return encodedKey;
	}

	public static PrivateKey loadPrivateKey(String string) throws GeneralSecurityException, IOException {
		PrivateKey key = loadKey("bitboxclient_rsa");
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keyspec = new PKCS8EncodedKeySpec(key.getEncoded());
		return (RSAPrivateKey) kf.generatePrivate(keyspec);
	}

	public static PublicKey loadPublicKey(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
		RSAPublicKeySpec decodedKey = decodeOpenSSH(publicKey);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		return factory.generatePublic(decodedKey);
	}

	public static String encrypt(String secretKey, PublicKey publicKey) throws Exception {
		Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
		byte[] cipherText = encryptCipher.doFinal(secretKey.getBytes());
		return Base64.getEncoder().encodeToString(cipherText);
	}

	public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
		byte[] bytes = Base64.getDecoder().decode(cipherText);
		Cipher decriptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);
		return new String(decriptCipher.doFinal(bytes),StandardCharsets.UTF_8);
	}

	public static PrivateKey loadKey(String keyFilePath) throws GeneralSecurityException, IOException {
		byte[] keyDataBytes = Files.readAllBytes(Paths.get(keyFilePath));
		String keyDataString = new String(keyDataBytes, StandardCharsets.UTF_8);
		keyDataString = keyDataString.replace(PKCS_1_PEM_HEADER, "");
		keyDataString = keyDataString.replace(PKCS_1_PEM_FOOTER, "");
		keyDataString = keyDataString.replace("\n", "").replace("\r", "");
		return readPkcs1PrivateKey(Base64.getDecoder().decode(keyDataString));

	}

	public static PrivateKey readPkcs8PrivateKey(byte[] pkcs8Bytes) throws GeneralSecurityException {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SunRsaSign");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
		try {
			return keyFactory.generatePrivate(keySpec);
		} catch (InvalidKeySpecException e) {
			throw new IllegalArgumentException("Unexpected key format!", e);
		}
	}

	public static PrivateKey readPkcs1PrivateKey(byte[] pkcs1Bytes) throws GeneralSecurityException {
		// We can't use Java internal APIs to parse ASN.1 structures, so we build a PKCS#8 key Java can understand
		int pkcs1Length = pkcs1Bytes.length;
		int totalLength = pkcs1Length + 22;
		byte[] pkcs8Header = new byte[] {
				0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff), // Sequence + total length
				0x2, 0x1, 0x0, // Integer (0)
				0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1, 0x1, 0x5, 0x0, // Sequence: 1.2.840.113549.1.1.1, NULL
				0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff) // Octet string + length
		};
		byte[] pkcs8bytes = join(pkcs8Header, pkcs1Bytes);
		return readPkcs8PrivateKey(pkcs8bytes);
	}

	public static byte[] join(byte[] byteArray1, byte[] byteArray2){
		byte[] bytes = new byte[byteArray1.length + byteArray2.length];
		System.arraycopy(byteArray1, 0, bytes, 0, byteArray1.length);
		System.arraycopy(byteArray2, 0, bytes, byteArray1.length, byteArray2.length);
		return bytes;
	}

	static RSAPublicKeySpec decodeOpenSSH(String fields) {
		byte[] std = Base64.getDecoder().decode(fields);
		return decodeRSAPublicSSH(std);
	}
	static RSAPublicKeySpec decodeRSAPublicSSH(byte[] encoded) {
		ByteBuffer input = ByteBuffer.wrap(encoded);
		String type = string(input);
		if (!"ssh-rsa".equals(type)) throw new IllegalArgumentException("Unsupported type");
		BigInteger exp = sshint(input);
		BigInteger mod = sshint(input);
		if (input.hasRemaining()) throw new IllegalArgumentException("Excess data");
		return new RSAPublicKeySpec(mod, exp);
	}

	public static String string(ByteBuffer buf) {
		return new String(lenval(buf), Charset.forName("US-ASCII"));
	}

	public static BigInteger sshint(ByteBuffer buf) {
		return new BigInteger(+1, lenval(buf));
	}

	public static byte[] lenval(ByteBuffer buf) {
		byte[] copy = new byte[buf.getInt()];
		buf.get(copy);
		return copy;
	}
}
