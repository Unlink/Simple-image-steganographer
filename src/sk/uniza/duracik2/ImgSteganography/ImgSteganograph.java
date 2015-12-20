/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package sk.uniza.duracik2.ImgSteganography;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

/**
 *
 * @author Unlink
 */
public class ImgSteganograph {
	
	private BufferedImage aImg;
	
	private static final int HEADER = 0x8621951;

	public ImgSteganograph(File paImgageFile) throws IOException {
		aImg = ImageIO.read(paImgageFile);
	}
	
	public boolean encrypt(char[] key, byte[] message) throws EncryptionException {
		try {
			byte[] iv = generateIv();
			byte[] salt = generateIv();
			IvParameterSpec ivspec = new IvParameterSpec(iv);
			
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(key, salt, 65536, 128);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec keySpec = new SecretKeySpec(tmp.getEncoded(), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivspec);
			
			byte[] encrypted = cipher.doFinal(message);
			
			//Dlžka zašifrovaneho textu + iv + zasifrovany text + specialny header + salt
			if ((encrypted.length + 16 + 4 + 4 + 16) * 8 > (aImg.getWidth() * aImg.getHeight() * 3)) {
				return false;
			}
			
			ByteBuffer buffer = ByteBuffer.allocate(encrypted.length + 16 + 4 + 4 + 16);
			buffer.putInt(HEADER);
			buffer.putInt(encrypted.length);
			buffer.put(iv);
			buffer.put(salt);
			buffer.put(encrypted);
			writeToImage(buffer.array());
		}
		catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
			throw new EncryptionException("Chyba pri šifrovaní", ex);
		}
		return true;
		
	}

	public byte[] decrypt(char[] key) throws EncryptionException {
		
		byte[] data = readFromImage();
		if (data == null) {
			return null;
		}

		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.getInt();
		int size = buffer.getInt();
		byte[] iv = new byte[16];
		byte[] salt = new byte[16];
		buffer.get(iv);
		buffer.get(salt);
		byte[] encrypted = new byte[size];
		buffer.get(encrypted);
		
		try {
			IvParameterSpec ivSpec = new IvParameterSpec(iv);
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(key, salt, 65536, 128);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec keySpec = new SecretKeySpec(tmp.getEncoded(), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
			
			return cipher.doFinal(encrypted);
		}
		catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
			throw new EncryptionException("Chyba pri dešifrovaní", ex);
		}
	}
	
	public void save(File paFile) throws IOException {
		ImageIO.write(aImg, "png", paFile);
	}
	
    /**
     * Generates a random IV to be used in the encryption process
     * @return The IV's byte representation
     */
    private byte[] generateIv() {
        SecureRandom random = new SecureRandom();
        byte[] ivBytes = new byte[16];
        random.nextBytes(ivBytes);
        return ivBytes;
    }

	private void writeToImage(byte[] data) {
		int x = 0;
		int y = 0;
		int z = 0;
		int c = aImg.getRGB(x, y);
		
		for (byte b : data) {
			for (int i = 0; i < 8; i++) {
				
				if (((b>>i)&0x1) == 1) {
					c|=0x1<<(8*z);
				}
				else {
					c&=~(0x1<<(8*z));
				}
				
				z++;
				if (z > 2) {
					aImg.setRGB(x, y, c);
					z = 0;
					x++;
					if (x >= aImg.getWidth()) {
						x = 0;
						y++;
					}
					c = aImg.getRGB(x, y);
				}
			} 
		}
		aImg.setRGB(x, y, c);
	}
	
	private byte[] readFromImage() {
		byte[] meta = new byte[4 + 4 + 16 + 16];
		
		int x = 0;
		int y = 0;
		int z = 0;
		int c = aImg.getRGB(x, y);
		
		for (int j=0; j<meta.length; j++) {
			for (int i = 0; i < 8; i++) {
				if (((c>>(8*z)) & 0x1) == 1) {
					meta[j] |= 0x1 << i;
				}
				else {
					meta[j] &= ~(0x1 << i);
				}
				z++;
				if (z > 2) {
					z = 0;
					x++;
					if (x >= aImg.getWidth()) {
						x = 0;
						y++;
					}
					c = aImg.getRGB(x, y);
				}
			} 
		}
		
		ByteBuffer buffer = ByteBuffer.wrap(meta);
		if (buffer.getInt() != HEADER) {
			return null;
		}
		int size = buffer.getInt();
		
		//Overflow...
		if (size * 8 > (((aImg.getWidth() * aImg.getHeight()) * 3) - 24)) {
			return null;
		}
		
		byte[] message = new byte[size];
		for (int j=0; j<size; j++) {
			for (int i = 0; i < 8; i++) {
				if (((c>>(8*z)) & 0x1) == 1) {
					message[j] |= 0x1 << i;
				}
				else {
					message[j] &= ~(0x1 << i);
				}
				z++;
				if (z > 2) {
					z = 0;
					x++;
					if (x >= aImg.getWidth()) {
						x = 0;
						y++;
					}
					c = aImg.getRGB(x, y);
				}
			} 
		}
		byte[] result = new byte[meta.length+size];
		System.arraycopy(meta, 0, result, 0, meta.length);
		System.arraycopy(message, 0, result, meta.length, size);
		return result;
	}
	
}
