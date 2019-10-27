package net.covers1624.wt.util.download;

import net.covers1624.wt.api.tail.DownloadProgressTail;
import net.covers1624.wt.util.ColUtils;
import net.covers1624.wt.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.function.Predicate;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Inspired and vaguely based off https://github.com/michel-kraemer/gradle-download-task
 * <pre>
 * Notable differences:
 *  Wayyy simpler implementation.
 *  Lazy evaluation of file and URL inputs.
 *  Single file downloads.
 *  External validation of file for up-to-date checking.
 *  UserAgent spoofing. (Thanks mojang!)
 *  Ability to set the ProgressLogger to use.
 * </pre>
 *
 * This is split into an Action, Spec and Task.
 *
 * The Spec {@link DownloadSpec}, Provides the specification for how things work.
 *
 * The Action {@link DownloadAction}, What actually handles downloading
 * implements {@link DownloadSpec}, Useful for other tasks that need to download
 * something but not necessarily create an entire task to do said download.
 *
 * implements {@link DownloadSpec} and hosts the Action as a task.
 *
 * Created by covers1624 on 8/02/19.
 */
public class DownloadAction implements DownloadSpec {

    private static final Logger logger = LogManager.getLogger("DownloadAction");

    private Object src;
    private Path dest;
    private boolean onlyIfModified;
    private UseETag useETag = UseETag.FALSE;
    private Path eTagFile;
    private String userAgent;
    private boolean quiet;
    private Predicate<Path> fileUpToDate = e -> true;

    private DownloadProgressTail progressTail;

    private boolean upToDate;

    public DownloadAction() {
    }

    static {
        //+'s break some amazon services for _some_reason.
        try {
            BitSet PATHSAFE = Utils.getField(URLEncodedUtils.class.getDeclaredField("PATHSAFE"), null);
            PATHSAFE.clear('+');
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Unable to reflect URLEncodedUtils.", e);
        }
    }

    public void execute() throws IOException {
        if (src == null) {
            throw new IllegalArgumentException("Download source not provided");
        }
        if (dest == null) {
            throw new IllegalArgumentException("Download destination not provided.");
        }

        URL src = getSrc();
        Path dest = getDest();

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(src.toString());
            long timestamp = 0;
            if (Files.exists(dest)) {
                timestamp = Files.getLastModifiedTime(dest).toMillis();
            }
            if (onlyIfModified && Files.exists(dest)) {
                request.addHeader("If-Modified-Since", DateUtils.formatDate(new Date(timestamp)));
            }
            if (getUseETag().isEnabled()) {
                String etag = loadETag(src);
                if (!getUseETag().weak && StringUtils.startsWith(etag, "W/")) {
                    etag = null;
                }
                if (etag != null) {
                    request.addHeader("If-None-Match", etag);
                }
            }
            request.addHeader("Accept-Encoding", "gzip");
            if (getUserAgent() != null) {
                request.addHeader("User-Agent", getUserAgent());
            }

            try (CloseableHttpResponse response = client.execute(request)) {
                int code = response.getStatusLine().getStatusCode();
                if ((code < 200 || code > 299) && code != HttpStatus.SC_NOT_MODIFIED) {
                    throw new ClientProtocolException(response.getStatusLine().getReasonPhrase());
                }
                long lastModified = 0;
                Header lastModifiedHeader = response.getLastHeader("Last-Modified");
                if (lastModifiedHeader != null) {
                    String val = lastModifiedHeader.getValue();
                    if (!StringUtils.isEmpty(val)) {
                        Date date = DateUtils.parseDate(val);
                        if (date != null) {
                            lastModified = date.getTime();
                        }
                    }
                }
                if ((code == HttpStatus.SC_NOT_MODIFIED || (lastModified != 0 && timestamp >= lastModified)) && fileUpToDate.test(dest)) {
                    if (!isQuiet()) {
                        logger.info("Not Modified. Skipping '{}'.", src);
                    }
                    upToDate = true;
                    return;
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return;//kden..
                }

                String humanSize = "";
                long contentLen = entity.getContentLength();
                if (contentLen >= 0) {
                    humanSize = toLengthText(contentLen);
                }
                long processed = 0;
                if (progressTail != null) {
                    progressTail.setTotalLen(contentLen);
                    progressTail.setStatus(DownloadProgressTail.Status.DOWNLOADING);
                    progressTail.setStartTime(System.currentTimeMillis());
                }
                boolean finished = false;
                Path dstTmp = dest.resolveSibling("__tmp_" + dest.getFileName());
                if (Files.notExists(dstTmp.getParent())) {
                    Files.createDirectories(dstTmp.getParent());
                }
                try (InputStream is = entity.getContent()) {
                    try (OutputStream os = Files.newOutputStream(dstTmp, CREATE)) {
                        byte[] buffer = new byte[16384];
                        int len;
                        while ((len = is.read(buffer)) >= 0) {
                            os.write(buffer, 0, len);
                            processed += len;
                            if (progressTail != null) {
                                progressTail.setProgress(processed);
                            }
                        }
                        os.flush();
                        finished = true;
                    }
                } finally {
                    if (!finished) {
                        Files.delete(dstTmp);
                    } else {
                        Files.move(dstTmp, dest, REPLACE_EXISTING);
                        if (Files.notExists(dest.getParent())) {
                            Files.createDirectories(dest.getParent());
                        }
                    }
                    if (progressTail != null) {
                        progressTail.setStatus(DownloadProgressTail.Status.IDLE);
                    }
                }
                if (onlyIfModified && lastModified > 0) {
                    Files.setLastModifiedTime(dest, FileTime.fromMillis(lastModified));
                }
                if (getUseETag().isEnabled()) {
                    Header eTagHeader = response.getFirstHeader("ETag");
                    if (eTagHeader != null) {
                        String etag = eTagHeader.getValue();
                        boolean isWeak = StringUtils.startsWith(etag, "W/");
                        if (isWeak && getUseETag().warnOnWeak && !quiet) {
                            logger.warn("Weak ETag found.");
                        }
                        if (!isWeak || getUseETag().weak) {
                            saveETag(src, etag);
                        }
                    }
                }
            }
        }
    }

    protected String loadETag(URL url) {
        Path eTagFile = getETagFile();
        if (Files.notExists(eTagFile)) {
            return null;
        }
        try {
            return ColUtils.head(Files.readAllLines(eTagFile));//Files.asCharSource(eTagFile, UTF_8).read();
        } catch (IOException e) {
            logger.warn("Error reading ETag file '{}'.", eTagFile);
            return null;
        }
    }

    protected void saveETag(URL url, String eTag) {
        Path eTagFile = getETagFile();
        try {
            Path tmp = eTagFile.resolveSibling("__tmp_" + eTagFile.getFileName());
            Files.write(tmp, Collections.singleton(eTag), CREATE);
            Files.move(tmp, eTagFile, REPLACE_EXISTING);
        } catch (IOException e) {
            logger.warn("Error saving ETag file '{}'.", eTagFile, e);
        }
    }

    private String toLengthText(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    @Override
    public void fileUpToDateWhen(Predicate<Path> spec) {
        fileUpToDate = fileUpToDate.and(spec);
    }

    //@formatter:off
    @Override public URL getSrc() { return makeURL(src); }
    @Override public Path getDest() { return dest; }
    @Override public boolean getOnlyIfModified() { return onlyIfModified; }
    @Override public UseETag getUseETag() { return useETag; }
    @Override public Path getETagFile() { return getETagFile_(); }
    @Override public String getUserAgent() { return userAgent; }
    @Override public boolean isQuiet() { return quiet; }
    @Override public boolean isUpToDate() { return upToDate; }
    @Override public void setSrc(Object src) { this.src = src; }
    @Override public void setDest(Path dest) { this.dest = dest; }
    @Override public void setOnlyIfModified(boolean onlyIfModified) { this.onlyIfModified = onlyIfModified; }
    @Override public void setUseETag(Object eTag) { this.useETag = UseETag.parse(eTag); }
    @Override public void setETagFile(Path eTagFile) { this.eTagFile = eTagFile; }
    @Override public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    @Override public void setQuiet(boolean quiet) { this.quiet = quiet; }
    @Override public void setProgressTail(DownloadProgressTail progressTail) { this.progressTail = progressTail; }
    //@formatter:on

    private Path getETagFile_() {
        if (eTagFile == null) {
            Path dest = getDest();
            return dest.resolveSibling(dest.getFileName() + ".etag");
        }
        return eTagFile;
    }

    private URL makeURL(Object object) {
        if (object instanceof String) {
            try {
                return new URL((String) object);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid URL " + object, e);
            }
        } else if (object instanceof URL) {
            return (URL) object;
        } else {
            throw new IllegalArgumentException("Expected String or URL. Got: " + object.getClass());
        }
    }

    public enum UseETag {
        FALSE(false, false),
        TRUE(true, true),
        ALL(true, false),
        STRONG(false, false);

        public final boolean weak;
        public final boolean warnOnWeak;

        UseETag(boolean weak, boolean warnOnWeak) {
            this.weak = weak;
            this.warnOnWeak = warnOnWeak;
        }

        public boolean isEnabled() {
            return this != FALSE;
        }

        public static UseETag parse(Object value) {
            if (value instanceof UseETag) {
                return (UseETag) value;
            } else if (value instanceof Boolean) {
                if ((Boolean) value) {
                    return TRUE;
                } else {
                    return FALSE;
                }
            } else if (value instanceof String) {
                switch ((String) value) {
                    case "true":
                        return TRUE;
                    case "false":
                        return FALSE;
                    case "all":
                        return ALL;
                    case "strong":
                        return STRONG;
                }
            }
            throw new IllegalArgumentException("Unable to parse ETag, Unknown value: " + value.toString());
        }
    }

}
