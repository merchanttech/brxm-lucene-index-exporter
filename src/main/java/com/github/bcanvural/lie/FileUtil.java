package com.github.bcanvural.lie;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.bcanvural.lie.DateUtil.AMSTERDAM_ZONE_ID;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.io.filefilter.TrueFileFilter.TRUE;

public final class FileUtil {

    public static final String DATE_FORMAT_YYYYMMDD = "yyyyMMdd";
    public static final String ZIP_EXTENSION = ".zip";

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    public static void copyFiles(String sourcePath, String destinationPath, String fileExtension) throws IOException {
        final List<String> files = Files.walk(Paths.get(sourcePath))
                .map(Path::toString)
                .filter(f -> f.endsWith(fileExtension))
                .collect(Collectors.toList());
        if (files.size() > 0) {
            for (String file : files) {
                Path source = Paths.get(file);
                String destinationFileName = DateUtil.formatCurrentDateToString(DATE_FORMAT_YYYYMMDD) + ZIP_EXTENSION;
                Path destination = Paths.get(destinationPath + destinationFileName);
                if (!Files.exists(destination)) {
                    Files.createDirectories(destination.getParent());
                }
                Files.copy(source, destination, REPLACE_EXISTING);
                log.info("Copied index file from live location {} to back up location {}", source, destination);
            }
        } else {
            log.info("There's no file at given liveindex location {} to back up", sourcePath);
        }
    }

    public static void deleteOlderFiles(String folderPath, int retentionDays) {
        LocalDate thresholdDate = LocalDate.now(ZoneId.of(AMSTERDAM_ZONE_ID)).minusDays(retentionDays);
        Date date = DateUtil.convertLocalDateToDateViaInstant(thresholdDate);
        File file = new File(folderPath);
        Iterator<File> filesToDelete = FileUtils.iterateFiles(file, new AgeFileFilter(date), TRUE);
        while (filesToDelete.hasNext()) {
            File fileToDelete = filesToDelete.next();
            if (fileToDelete.delete()) {
                log.info("Successfully deleted file {}", fileToDelete);
            } else {
                log.error("Unable to delete file {}", fileToDelete);
            }
        }
    }

    public static void moveFileAtomically(File file, String path) throws IOException {
        Path source = Paths.get(file.getAbsolutePath());
        Path destination = Paths.get(path);
        if (!Files.exists(destination)) {
            Files.createDirectories(destination.getParent());
        }
        Files.move(source, destination, ATOMIC_MOVE);
        log.info("Atomically moved {} to {} ", source, destination);
    }

    public static File createTemporaryFile(String tempDirectory, String tempFileNamePrefix) throws IOException {
        File tempPath = new File(tempDirectory);
        if (!tempPath.exists()) {
            tempPath.mkdirs();
        }
        return File.createTempFile(tempFileNamePrefix, null, tempPath);
    }

    public static void copyStreamToFile(File file, InputStream inputStream) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        StreamUtils.copy(inputStream, fos);
        fos.close();
    }

}
