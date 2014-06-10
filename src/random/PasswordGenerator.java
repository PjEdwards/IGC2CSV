/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package random;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 *
 * @author pedwards
 */
public class PasswordGenerator {
    public static void main(String[] argv) {
        SecureRandom random = new SecureRandom();
        System.out.println(new BigInteger(130, random).toString(32));
    }
}
