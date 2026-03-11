package br.com.mypersonalmoney.security;

import io.quarkus.elytron.security.common.BcryptUtil;

public class PasswordTool {

    public static void main(String[] args) {
        String hash = BcryptUtil.bcryptHash("Ferr@r31");
        System.out.println(hash);
    }
}