package net.minecraftforge.userdev;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.WillNotClose;
import java.io.*;
import java.net.Proxy;

/**
 * Created by covers1624 on 27/10/19.
 */
public class WTLaunchLogin {

    private static final Logger LOGGER = LogManager.getLogger("LaunchLogin");
    private static final ThreadLocal<byte[]> bufferCache = ThreadLocal.withInitial(() -> new byte[32 * 1024]);
    private static final File loginMetaFile = new File(System.getProperty("wt.login_meta", ".login_meta"));
    private static final byte[] xorKey = System.getProperty("wt.xor_key", "spoopy skeletons").getBytes(Charsets.UTF_8);

    public static void main(String[] args) throws Throwable {
        ArgumentList argList = ArgumentList.from(args);
        LoginMeta loginMeta = LoginMeta.parseFile(loginMetaFile);
        YggdrasilUserAuthentication auth = tryLogin(loginMeta, false);
        argList.put("accessToken", auth.getAuthenticatedToken());
        argList.put("uuid", auth.getSelectedProfile().getId().toString().replace("-", ""));
        argList.put("username", auth.getSelectedProfile().getName());
        argList.put("userType", auth.getUserType().getName());
        Gson gson = new GsonBuilder().registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer()).create();
        argList.put("userProperties", gson.toJson(auth.getUserProperties()));
        LOGGER.info("Logged in!");
        LaunchTesting.main(argList.getArguments());
    }

    public static YggdrasilUserAuthentication tryLogin(LoginMeta loginMeta, boolean forceAsk) {
        if (loginMeta.username.isEmpty() || loginMeta.password.isEmpty()) {
            forceAsk = true;
        }
        if (forceAsk) {
            Pair<String, String> newLogin = promptLogin(Pair.of(loginMeta.username, loginMeta.password));
            loginMeta.username = newLogin.getLeft();
            loginMeta.password = newLogin.getRight();
            LoginMeta.writeToFile(loginMetaFile, loginMeta);
        }
        YggdrasilUserAuthentication auth = (YggdrasilUserAuthentication) new YggdrasilAuthenticationService(Proxy.NO_PROXY, "1").createUserAuthentication(Agent.MINECRAFT);
        auth.setUsername(loginMeta.username);
        auth.setPassword(loginMeta.password);
        try {
            auth.logIn();
        } catch (AuthenticationException e) {
            LOGGER.error("Login failed!", e);
            return tryLogin(loginMeta, true);
        }
        return auth;
    }

    private static Pair<String, String> promptLogin(Pair<String, String> current) {
        WTCredentialsDialog dialog = new WTCredentialsDialog();
        dialog.pack();
        dialog.setUsername(current.getLeft());
        dialog.setPassword(current.getRight());
        dialog.setVisible(true);
        return Pair.of(dialog.getUsername(), dialog.getPassword());
    }

    //Yes this data is xor'd to disk.
    //No that is NOT encryption, the key is known.
    //Keep the file safe, do not send it to ANYONE.
    //This feels safer than plain text
    private static class LoginMeta {

        private String username = "";
        private String password = "";

        public void read(DataInput input, int version) throws IOException {
            username = input.readUTF();
            password = input.readUTF();
        }

        public void write(DataOutput output) throws IOException {
            output.writeUTF(username);
            output.writeUTF(password);
        }

        public static LoginMeta parseFile(File file) {
            if (file.exists()) {
                try {
                    byte[] bytes;
                    try (InputStream is = new FileInputStream(file)) {
                        bytes = xor(toBytes(is), xorKey);
                    }
                    DataInput input = new DataInputStream(new ByteArrayInputStream(bytes));
                    LoginMeta loginMeta = new LoginMeta();
                    loginMeta.read(input, input.readInt());
                    return loginMeta;
                } catch (IOException e) {
                    LOGGER.warn("Failed to read .login_meta, Starting from scratch.", e);
                }
            }
            return new LoginMeta();
        }

        public static void writeToFile(File file, LoginMeta loginMeta) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutput output = new DataOutputStream(out);
                output.writeInt(1);
                loginMeta.write(output);
                try (FileOutputStream fOut = new FileOutputStream(makeFile(file))) {
                    fOut.write(xor(out.toByteArray(), xorKey));
                }
            } catch (IOException e) {
                LOGGER.fatal("Failed to write LoginMeta. File: '{}'.", file, e);
            }
        }
    }

    public static byte[] xor(byte[] in, byte[] key) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (byte) (in[i] ^ key[i % key.length]);
        }
        return out;
    }

    public static byte[] toBytes(@WillNotClose InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copy(is, os);
        return os.toByteArray();
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = bufferCache.get();
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    @SuppressWarnings ("ResultOfMethodCallIgnored")
    public static File makeFile(File file) {
        if (!file.exists()) {
            File p = file.getAbsoluteFile().getParentFile();
            if (!p.exists()) {
                p.mkdirs();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create a new file.", e);
            }
        }
        return file;
    }
}
