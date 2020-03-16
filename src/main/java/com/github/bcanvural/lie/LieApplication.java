package com.github.bcanvural.lie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.github.bcanvural.lie.FileUtil.ZIP_EXTENSION;

@SpringBootApplication
public class LieApplication implements CommandLineRunner {

    @Value("${LIE_URL}")
    private String url;

    @Value("${LIE_USER}")
    private String username;

    @Value("${LIE_PASSWORD}")
    private String password;

    @Value("${LIE_LIVE_INDEX_DIR}")
    private String liveIndexDir;

    @Value("${LIE_BACKUP_INDEX_DIR}")
    private String backupIndexDir;

    @Value("${LIE_TEMP_INDEX_DIR}")
    private String tempIndexDir;

    @Value("${LIE_MAX_RETRIES}")
    private int maxRetries;

    @Value("${LIE_INDEX_RETENTION_DAYS}")
    private int indexRetentionDays;

    @Value("${LIE_RETRY_INTERVAL_MS}")
    private long retryInterval;

    private static final String TEMP_LUCENE_INDEX_FILE_PREFIX = "tempLuceneIndex";

    private static final Logger log = LoggerFactory.getLogger(LieApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(LieApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        for (int retryNum = 0; retryNum <= maxRetries; retryNum++) {
            try {
                log.debug("Invoked LuceneIndexExport for url {} ", url);
                final File tempZipFile = FileUtil.createTemporaryFile(tempIndexDir, TEMP_LUCENE_INDEX_FILE_PREFIX);
                try {
                    RestTemplate template = new RestTemplate();
                    template.execute(url, HttpMethod.GET, request -> {
                        String auth = username + ":" + password;
                        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
                        String authHeader = "Basic " + new String(encodedAuth);
                        request.getHeaders().set("Authorization", authHeader);
                    }, response -> {
                        FileUtil.copyStreamToFile(tempZipFile, response.getBody());
                        return tempZipFile;
                    });
                    log.info("Lucene index was downloaded successfully");
                    String liveIndexPath = liveIndexDir + "liveindex.zip"; //e.g. /mnt/volume/liveindex/liveindex.zip
                    log.info("Attempting an atomic swap from temp lucene download location at {} to live index at {}", tempZipFile.getAbsolutePath(), liveIndexPath);
                    FileUtil.moveFileAtomically(tempZipFile, liveIndexPath);
                    log.info("File move from temp lucene download location at {} to live index at {} was successful.", tempZipFile.getAbsolutePath(), liveIndexPath);
                } finally {
                    log.info("Removing temp file from {}", tempZipFile.getPath());
                    tempZipFile.delete();
                }
                log.info("Exporting lucene index was successful");
                backupAllLiveIndices();
                return;
            }
            catch (ResourceAccessException | HttpClientErrorException | HttpServerErrorException | UnknownHttpStatusCodeException | IOException ex) {
                log.error("Failed to export lucene index using url {}, {} ", url, ex);
                retryNum++;
                log.debug("Current retry number is: {}", retryNum);
                Thread.sleep(retryInterval);
                if (retryNum == maxRetries) {
                    log.error("Failed to export index after trying {} times",  retryNum);
                    return;
                }
            }
        }
    }

    public void backupAllLiveIndices() {
        try {
            log.info("Backing up all live index files.");
            FileUtil.copyFiles(liveIndexDir, backupIndexDir, ZIP_EXTENSION);
            FileUtil.deleteOlderFiles(backupIndexDir, indexRetentionDays);
            log.info("Live index file/s backed up successfully");
        } catch (IOException ex) {
            log.error("Failed to backup index files... ", ex);
            throw new RuntimeException("Failed to backup index files.");
        }
    }
}
