package ru.ruranobe.misc;

import org.apache.wicket.util.string.Strings;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Encodes a string using MD5 hashing
 *
 * @author Rafael Steil
 * @version $Id: MD5.java,v 1.7 2006/08/23 02:13:44 rafaelsteil Exp $
 *          <p/>
 *          Copied from jforum sources.
 */
public class MD5
{

    public static String crypt(String str)
    {
        if (Strings.isEmpty(str))
        {
            throw new IllegalArgumentException("String to encript cannot be null or zero length");
        }

        StringBuffer hexString = new StringBuffer();

        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] hash = md.digest();

            for (int i = 0; i < hash.length; i++)
            {
                if ((0xff & hash[i]) < 0x10)
                {
                    hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
                } else
                {
                    hexString.append(Integer.toHexString(0xFF & hash[i]));
                }
            }
        } catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("" + e);
        }

        return hexString.toString();
    }
}
