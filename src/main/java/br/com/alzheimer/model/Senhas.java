package br.com.alzheimer.model;

import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Senhas {
    private static byte[] derivar(String senha, byte[] sal) {
        try {
            var spec = new PBEKeySpec(senha.toCharArray(), sal, 150000, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
    public static String criar(String senha) {
        if (senha.length() < 8 || senha.length() > 128) throw new IllegalArgumentException("Use de 8 a 128 caracteres na senha.");
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        var b64 = Base64.getEncoder();
        return b64.encodeToString(sal) + ":" + b64.encodeToString(derivar(senha, sal));
    }
    public static boolean conferir(String senha, String hash) {
        var partes = hash.split(":");
        var b64 = Base64.getDecoder();
        return partes.length == 2 && MessageDigest.isEqual(b64.decode(partes[1]), derivar(senha,b64.decode(partes[0])));
    }
}
