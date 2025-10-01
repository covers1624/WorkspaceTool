package net.covers1624.wstool.wrapper.http;

import net.covers1624.quack.net.httpapi.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 7/5/25.
 */
public final class Java8HttpEngineRequest extends AbstractEngineRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Java8HttpEngineRequest.class);

    private static final int MAX_REDIRECTS = Integer.getInteger("Java8HttpEngine.max_redirects", 5);

    private @Nullable String method;
    private @Nullable WebBody body;

    @Override
    public EngineRequest method(String method, @Nullable WebBody body) {
        this.method = method;
        this.body = body;
        return this;
    }

    @Override
    protected void assertState() {
        super.assertState();
        if (method == null) {
            throw new IllegalStateException("method(String, Body) must be called first");
        }
    }

    @Override
    public EngineResponse execute() throws IOException {
        assertState();
        executed = true;

        // TODO perhaps not?
        if (body != null && !body.multiOpenAllowed()) {
            throw new IllegalArgumentException("Body must support being opened multiple times due to redirects.");
        }

        String theUrl = requireNonNull(this.url, "URL not set.");

        for (int i = 0; i < MAX_REDIRECTS; i++) {
            URL url = new URL(theUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            for (HeaderList.Entry header : headers) {
                conn.addRequestProperty(header.name, header.value);
            }
            conn.setRequestMethod(method);
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(15));
            conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(15));
            if (body != null) {
//                if (listener != null) listener.start(RequestListener.Direction.UPLOAD);
                conn.setDoOutput(true);
                if (body.contentType() != null) {
                    conn.setRequestProperty("Content-Type", body.contentType());
                }
                try (OutputStream os = conn.getOutputStream();
                     InputStream is = body.open()) {
                    IOUtils.copy(is, os);
                }
            } else {
//                if (listener != null) listener.start(RequestListener.Direction.DOWNLOAD);
            }

            String redirect = calculateRedirect(conn, url);
            if (redirect != null) {
                theUrl = redirect;
                LOGGER.info("Following redirect to {}.", redirect);
                continue;
            }

            int code = conn.getResponseCode();
            String message = conn.getResponseMessage();
            InputStream is = conn.getInputStream();

            HeaderList responseHeaders = new HeaderList();
            Map<String, List<String>> fields = conn.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : fields.entrySet()) {
                if (entry.getKey() == null) continue;
                for (String value : entry.getValue()) {
                    responseHeaders.add(entry.getKey(), value);
                }
            }

            String contentType = responseHeaders.get("Content-Type");
            String len = responseHeaders.get("Content-Length");
            long contentLength = len != null && !len.isEmpty() ? Long.parseLong(len) : -1;

            // @formatter:off
            WebBody responseBody = new WebBody() {
                @Override public InputStream open() { return is; }
                @Override public boolean multiOpenAllowed() { return false; }
                @Override public long length() { return contentLength; }
                @Override public @Nullable String contentType() { return contentType; }
            };
            return new EngineResponse() {
                @Override public EngineRequest request() { return Java8HttpEngineRequest.this; }
                @Override public int statusCode() { return code; }
                @Override public String message() { return message; }
                @Override public HeaderList headers() { return responseHeaders; }
                @Override public WebBody body() { return responseBody; }
                @Override public void close() throws IOException { is.close(); }
            };
            // @formatter:on
        }
        throw new IOException("Too many redirects.");
    }

    private static boolean shouldFollowRedirect(int code) {
        return code >= 300 && code <= 307 && code != 306 && code != HTTP_NOT_MODIFIED;
    }

    private static @Nullable String calculateRedirect(HttpURLConnection conn, URL url) throws IOException {
        int code = conn.getResponseCode();
        String locHeader = conn.getHeaderField("Location");
        if (!shouldFollowRedirect(code) || locHeader == null) return null;

        URI uri = URI.create(locHeader);
        String redirect;
        if (!uri.isAbsolute()) {
            locHeader = URLDecoder.decode(locHeader, "UTF-8");
            redirect = new URL(url, locHeader).toExternalForm();
        } else {
            redirect = uri.toString();
        }

        try {
            conn.getInputStream().close();
        } catch (Throwable ignored) { }
        return redirect;
    }
}
