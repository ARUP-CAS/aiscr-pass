package cz.ssc.pass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {
    // based on cz.smartgis.megalit.Util.makeSHA1
    public static String makeSHA1(String str) 
            throws EmptyArgumentException, NoSuchAlgorithmException {
	if ((str == null) || str.isEmpty()) {
            throw new EmptyArgumentException("Password is empty.");
	}

	byte[] strB = str.getBytes();
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        sha.update(strB);
        byte[] mac = sha.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : mac) {
            sb.append(Character.forDigit((b & 240) >> 4, 16));
            sb.append(Character.forDigit((b & 15), 16));
	}
	
        return sb.toString();
    }
}
