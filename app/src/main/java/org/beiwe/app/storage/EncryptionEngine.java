package org.beiwe.app.storage;


import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;

import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**The EncryptionEngine handles all encryption and hashing duties for the app.
 * Per-file AES encryption keys are generated and encrypted with the provided RSA key.
 * The RSA key is provided by the administrating server.
 * Hashing uses the SHA256 hashing algorithm.
 * @author Eli Jones, Josh Zagorsky */
public class EncryptionEngine {
	
	// these variables and the two static function below have some interaction with the kotlin
	// conversion where the line
	// 			PersistentData.putCommit(HASH_SALT_KEY, new String(newSalt));
	// is converted to invalid Kotlin code. The reason is obscure So we moved it here instead.
	static final String HASH_SALT_KEY = "hash_salt_key";
	static final String HASH_ITERATIONS_KEY = "hash_iterations_key";
	static final String USE_ANONYMIZED_HASHING_KEY = "use_anonymized_hashing";
	
	// shouldn't we be generating a new salt every time we hash something? FIXME: test thiss
	// Get salt for pbkdf2 hashing
	public static byte[] getHashSalt() {
		String saltString = PersistentData.pref.getString(HASH_SALT_KEY, null);
		// create salt if it does not exist
		if(saltString == null) {
			byte[] newSalt = SecureRandom.getSeed(64);
			PersistentData.putCommit(HASH_SALT_KEY, new String(newSalt));
			return newSalt;
		}
		else {
			return saltString.getBytes();
		}
	}
	
	// Get iterations for pbkdf2 hashing
	public static int getHashIterations() {
		int iterations = PersistentData.pref.getInt(HASH_ITERATIONS_KEY, 0);
		// create iterations if it does not exist
		if(iterations == 0) {
			// create random iteration count from 900 to 1100
			int newIterations = 1100 - new Random().nextInt(200);
			PersistentData.putCommit(HASH_ITERATIONS_KEY, newIterations);
			return newIterations;
		}
		else {
			return iterations;
		}
	}
	
	private static PublicKey RSAkey = null;
	
	/*############################################################################
	 * ############################### Hashing ###################################
	 * #########################################################################*/
	
	/** Takes a string as input, handles the usual thrown exceptions, and return a hash string of that input.  
	 * @param input A String to hash
	 * @return a Base64 String of the hash result. */
	public static String safeHash (String input) {
		try {
			return unsafeHash( input ); }
		catch (NoSuchAlgorithmException e) {
			Log.e("Hashing function", "NoSuchAlgorithmException"); //not gonna happen
			e.printStackTrace();
			throw new NullPointerException("device is too stupid to live, crashed inside safeHash 1"); }
		catch (UnsupportedEncodingException e) {
			Log.e("Hashing function", "UnsupportedEncodingException"); //not gonna happen
			e.printStackTrace();
			throw new NullPointerException("device is too stupid to live, crashed inside safeHash 2"); }
	}
	
	
	/** Takes a string as input, outputs a hash.
	 * @param input A String to hash.
	 * @return a Base64 String of the hash result. */
	public static String unsafeHash (String input) throws NoSuchAlgorithmException, UnsupportedEncodingException{
		if (input == null ) { Log.e("Hashing", "The hash function received a null string, it should now crash...");}
		if (input.length() == 0) { return "null_data"; } //if an empty string is provided, return a string describing this.
		MessageDigest hash = null;
		hash = MessageDigest.getInstance("SHA-256");
		hash.update( input.getBytes("UTF-8") );
		return toBase64String( hash.digest() );		
	}

	/** Takes a string as input, outputs a PBKDF2 hash.
	 * @param input A String to hash.
	 * @return a Base64 String of the hash result. */
	public static String PBKDF2Hash (String input) {
		byte[] salt = getHashSalt();
		int iterations = getHashIterations();
		PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
		generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(input.toCharArray()), salt, iterations);

		// keySize is 512 because we think it is the size of the digest
		KeyParameter key = (KeyParameter)generator.generateDerivedMacParameters(512);
		return toBase64String(key.getKey());
	}
	
	/**Converts a phone number into a 64-character hexadecimal string.
	 * First standardizes the phone numbers by grabbing the last 10 digits, so
	 * that hopefully, two identical phone numbers will get identical hashes,
	 * even if one has dashes and a country code and the other doesn't.
	 * 
	 * Grabbing the last 10 numerical characters is much simpler than using something like this:
	 * https://github.com/googlei18n/libphonenumber
	 * 
	 * @param phoneNumber a string representing a phone number
	 * @return a hexadecimal string, or an error message string */
	public static String hashPhoneNumber(String phoneNumber) {

		// Strip from the string any characters that aren't digits 0-9
		String justDigits = phoneNumber.replaceAll("\\D+", "");

		// Grab the last 10 digits
		String last10;
		if (justDigits.length() > 10) {
			last10 = justDigits.substring(justDigits.length() - 10); }
		else { last10 = justDigits; }

		// Hash the last 10 digits

		if (PersistentData.getUseAnonymizedHashing()) {
			return PBKDF2Hash(last10);
		}
		else {
			return safeHash(last10);
		}
	}

	public static String hashMAC(String MAC) {
		if (PersistentData.getUseAnonymizedHashing()) {
			return PBKDF2Hash(MAC);
		}
		else {
			return safeHash(MAC);
		}
	}
	
	
	/*############################################################################
	 * ############################ Encryption ###################################
	 * #########################################################################*/
	
	
	/**Encrypts data using the RSA cipher and the public half of an RSA key pairing provided by the server. 
	 * @param data to be encrypted
	 * @return a hex string of the encrypted data. */
	@SuppressLint("TrulyRandom")
	public static String encryptRSA(byte[] data) throws InvalidKeySpecException {
		if (RSAkey == null) readKey();
		
		//unfortunately we have problems encrypting this data, it occasionally loses a character, so we need to
		// base64 encode it first.
		data = toBase64Array(data);
		
		byte[] encryptedText = null;
		Cipher rsaCipher = null;
		
		try { rsaCipher = Cipher.getInstance("RSA"); }
		catch (NoSuchAlgorithmException e) {
			Log.e("Encryption Engine", "THIS DEVICE DOES NOT SUPPORT RSA");
			throw new NullPointerException("device is too stupid to live");}
		catch (NoSuchPaddingException e) {
			Log.e("Encryption Engine", "Device does not reconize padding format.  this is interesting because there ISN'T ONE (instance 1)");
			throw new NullPointerException("device is too stupid to live");}
		
		try { rsaCipher.init(Cipher.ENCRYPT_MODE, RSAkey);	}
		catch (InvalidKeyException e) {
			Log.e("Encryption Engine", "The key is not a valid public RSA key.");
			throw new NullPointerException("the RSA key was not valid");
		} //will crash soon
		
		try { encryptedText = rsaCipher.doFinal( data ); }
		catch (IllegalBlockSizeException e1) { Log.e("Encryption Engine", "The key is malformed.");
			// NOTE FOR FUTURE UPGRADES TO THIS SOFTWARE.
			// The only solution to this error is to uninstall and reregister the device,
			// or build entirely new functionality (no.) to redownload the key.
			// This seems like it would be a security headache, so let's not attempt that.
			throw new NullPointerException("RSA Key is invalid, the user needs to reregister their device."); } 
		catch (BadPaddingException e2) { 
			Log.e("Encryption Engine", "Device does not reconize padding format.  this is interesting because there ISN'T ONE (instance 2)");
			throw new NullPointerException("device is too stupid to live"); }
		
		return toBase64String(encryptedText);
	}
	
	
	/**Encrypts data using provided AES key
	 * @param plainText Any plain text data.
	 * @param aesKey A byte array, must contain 128 bits, used as the AES key.
	 * @return a string containing colon separated url-safe Base64 encoded data. First value is the Initialization Vector, second is the encrypted data.
	 * @throws InvalidKeyException
	 * @throws InvalidKeySpecException */
	public static String encryptAES(String plainText, byte[] aesKey) throws InvalidKeyException, InvalidKeySpecException { return encryptAES( plainText.getBytes(), aesKey ); }
	
	public static String encryptAES(byte[] plainText, byte[] aesKey) throws InvalidKeyException, InvalidKeySpecException {
		if (RSAkey == null) readKey(); 
		
		//create an iv, 16 bytes of data
		SecureRandom random = new SecureRandom();
		IvParameterSpec ivSpec = new IvParameterSpec( random.generateSeed(16) );
		
		//initialize an AES encryption cipher, we are using CBC mode.
		SecretKeySpec secretKeySpec = new SecretKeySpec( aesKey, "AES" );
		Cipher cipher = null;
		try { cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); }
		catch (NoSuchAlgorithmException e) { // seems unlikely and should fail at the previous AES
			Log.e("Encryption Engine", "device does not know what AES is, instance 2" );
			e.printStackTrace();
			throw new NullPointerException("device is too stupid to live"); }
		catch (NoSuchPaddingException e) { //seems unlikely
			Log.e("Encryption Engine", "device does not know what PKCS5 padding is" );
			e.printStackTrace();
			throw new NullPointerException("device is too stupid to live"); } 
		try { cipher.init( Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec ); }
		catch (InvalidAlgorithmParameterException e) { //seems unlikely, iv generation failed?
			Log.e("Encryption Engine", "InvalidAlgorithmParameterException during AES encryption..." );
			e.printStackTrace();
			throw new NullPointerException("InvalidAlgorithmParameterException during AES encryption..."); }
		
		//encrypt the data
		try { return toBase64String( ivSpec.getIV() ) + ":" +
					 toBase64String( cipher.doFinal( plainText ) ); }
		catch (IllegalBlockSizeException e) { //not possible, block size is coded to use the pkcs5 spec
			Log.e("Encryption Engine", "an impossible error ocurred" );
			e.printStackTrace(); 
			throw new NullPointerException("device is too stupid to live"); }
		catch (BadPaddingException e) {
			Log.e("Encryption Engine", "an unknown error occured in AES padding" );
			e.printStackTrace(); 
			throw new NullPointerException("an unknown error occured in AES encryption."); }
	}
	
	/* #######################################################################
	 * ########################## Key Management #############################  
	 * #####################################################################*/
	
	/**Checks for and reads in the RSA key file. 
	 * @throws InvalidKeySpecException Thrown most commonly when there is no key file, this is expected behavior.*/
	public static void readKey() throws InvalidKeySpecException {
		String key_content = TextFileManager.getKeyFile().read();
		byte[] key_bytes = Base64.decode(key_content, Base64.DEFAULT);
		X509EncodedKeySpec x509EncodedKey = new X509EncodedKeySpec( key_bytes );
		
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			RSAkey = keyFactory.generatePublic( x509EncodedKey ); }
		catch (NoSuchAlgorithmException e1) {
			Log.e("Encryption Engine", "ENCRYPTION HAS FAILED BECAUSE RSA IS NOT SUPPORTED?");
			e1.printStackTrace();
			throw new NullPointerException("ENCRYPTION HAS FAILED BECAUSE RSA IS NOT SUPPORTED?"); }
		catch (InvalidKeySpecException e2) {
			Log.e("Encryption Engine", "The provided RSA public key is NOT VALID." );
			throw e2; }
	}
	
	/**Generates a new 128 bit AES Encryption key.
	 * @return a byte array 128 bits long for use as an AES Encryption key*/
	public static byte[] newAESKey() {
		// setup seed and key generator
		SecureRandom random = new SecureRandom();
		KeyGenerator aesKeyGen = null;
		byte[] secretKey = null;
		
		while (true) {
			try {
				aesKeyGen = KeyGenerator.getInstance("AES");
			} catch (NoSuchAlgorithmException e) { //seems unlikely
				Log.e("Encryption Engine", "device does not know what AES is... instance 1");
				e.printStackTrace();
				throw new NullPointerException("device does not know what AES is... instance 1");
			}
			
			//from key generator, generate a key!
			aesKeyGen.init( 128, random );
			secretKey = aesKeyGen.generateKey().getEncoded();
			
			// helper code for debugging 0 values
			// byte[] ba = new byte[]{0};
			// Log.w("encryption engine", "example byte 64: " + toBase64String(ba));
			
			// We discovered that key values starting with 0 value byte causes problems, so we have implemented a safety test.
			// TODO: we need a better keying process, ECC and real key padding.  Substantial rework.
			if (secretKey[0] != 0) {
				// Log.e("encryption engine", "new key! = " + toBase64String(secretKey));
				return secretKey;
			}
			// Log.e("encryption engine", "bad key! = " + toBase64String(secretKey));
		}
	}
	
	
	/* #######################################################################
	 * ########################## Data Wrapping ##############################  
	 * #####################################################################*/

	/* converts data into url-safe Base64 encoded blobs, as either a string or a byte array. */
	private static String toBase64String( byte[] data ) { return Base64.encodeToString(data, Base64.NO_WRAP | Base64.URL_SAFE ); }
//	private static String toBase64String( String data ) { return Base64.encodeToString(data.getBytes(), Base64.NO_WRAP | Base64.URL_SAFE ); }
	private static byte[] toBase64Array( byte[] data ) { return Base64.encode(data, Base64.NO_WRAP | Base64.URL_SAFE ); }
//	private static byte[] toBase64Array( String data ) { return Base64.encode(data.getBytes(), Base64.NO_WRAP | Base64.URL_SAFE ); }
}