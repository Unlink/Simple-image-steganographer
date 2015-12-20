/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package sk.uniza.duracik2.ImgSteganography;

/**
 *
 * @author Unlink
 */
public class EncryptionException extends Exception {

	public EncryptionException(String paMessage) {
		super(paMessage);
	}

	public EncryptionException(String paMessage, Throwable paCause) {
		super(paMessage, paCause);
	}
	
}
