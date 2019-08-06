/*
 * Copyright (c) 2018-2019 covers1624
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import net.covers1624.wt.gstart.CredentialsDialog;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.WillNotClose;
import java.io.*;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 29/01/19.
 */
public class GradleStartLogin extends GradleStart {

    private static final ThreadLocal<byte[]> bufferCache = ThreadLocal.withInitial(() -> new byte[32 * 1024]);
    private static final File loginMetaFile = new File(System.getProperty("wt.login_meta", ".login_meta"));
    private static final byte[] xorKey = System.getProperty("wt.xor_key", "spoopy skeletons").getBytes(Charsets.UTF_8);

    public static void main(String[] args) throws Throwable {
        Method m_hackNatives = GradleStart.class.getDeclaredMethod("hackNatives");
        m_hackNatives.setAccessible(true);
        m_hackNatives.invoke(null);

        new GradleStartLogin().launch(args);
    }

    @Override
    protected void preLaunch(Map<String, String> argMap, List<String> extras) {
        LoginMeta loginMeta = LoginMeta.parseFile(loginMetaFile);
        YggdrasilUserAuthentication auth = tryLogin(loginMeta, false);
        argMap.put("accessToken", auth.getAuthenticatedToken());
        argMap.put("uuid", auth.getSelectedProfile().getId().toString().replace("-", ""));
        argMap.put("username", auth.getSelectedProfile().getName());
        argMap.put("userType", auth.getUserType().getName());
        Gson gson = new GsonBuilder().registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer()).create();
        argMap.put("userProperties", gson.toJson(auth.getUserProperties()));

    }

    public YggdrasilUserAuthentication tryLogin(LoginMeta loginMeta, boolean forceAsk) {
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

    private Pair<String, String> promptLogin(Pair<String, String> current) {
        CredentialsDialog dialog = new CredentialsDialog();
        dialog.pack();
        dialog.setUsername(current.getLeft());
        dialog.setPassword(current.getRight());
        dialog.setVisible(true);
        return Pair.of(dialog.getUsername(), dialog.getPassword());
    }

    //Yes this data is xor'd to disk.
    //No that is NOT encryption, the key is known.
    //Keep the file safe, do not send it to ANYONE.
    //This feels safer than plain text and protects
    //against accidentally opening the file on stream
    //or in a screen capture.
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
