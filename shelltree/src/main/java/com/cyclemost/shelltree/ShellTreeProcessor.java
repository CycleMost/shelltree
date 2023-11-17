package com.cyclemost.shelltree;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dbridges
 */
public class ShellTreeProcessor {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ShellTreeProcessor.class);
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HHmmss"); 
  public static List<String> CONFIG_FILE_NAMES = Arrays.asList(new String[] {"shelltree.properties", ".shelltree"});

  /**
   * Processes the specified path. The entire directory tree under the
   * specified path will be processed, recursively.
   * 
   * @param path root path to begin processing
   * @param parentConfig options of parent folder, or null for none
   */
  public static void process(String path, PathConfig parentConfig) {
    File configFile = null;
    for (String configFileName : CONFIG_FILE_NAMES) {
      Path configPath = Paths.get(path, configFileName);
      if (configPath.toFile().exists()) {
        configFile = configPath.toFile();
        break;
      }
    }
    PathConfig config = null;
    try {
      if (configFile != null) {
        // config file exists in this folder; use those values.
        // cascade parent values?
        config = new PathConfig(configFile);
      }
      else {
        // no config file in this folder; use parent config
        config = parentConfig;
      }
      
      // If we have config, perform actions on this path
      if (config != null) {
        performActions(path, config);

        // Process subfolders
        var files = Paths.get(path).toFile().listFiles();
        for (var file : files) {
          if (file.isDirectory() && 
             !file.getName().startsWith(".") &&
             !CONFIG_FILE_NAMES.contains(file.getName()) &&
              file.getName().compareToIgnoreCase(config.getArchiveFolder()) != 0)
          {
            process(file.getAbsolutePath(), 
                    config.isRecursive() ? config : null);
          }
        }          
      }
      else {
        LOGGER.debug("no config for {}", path);
      }
    }
    catch (Exception ex) {
      LOGGER.error("Error processing path {};", path, ex);
    }
    
  }
  
  /**
   * Performs actions specified by the active config file for this path.
   * 
   * @param path
   * @param config 
   */
  static void performActions(String path, PathConfig config) throws IOException {
    LOGGER.debug("Actions for {}: {}", path, config);
    
    File archivePath = null;
    String archiveName = String.format("archive-%s.zip", DATE_FORMAT.format(new Date()));
    if (!StringUtils.isBlank(config.getArchiveFolder())) {
      Path archiveFolderPath = Paths.get(path, config.getArchiveFolder());
      archivePath = Paths.get(path, config.getArchiveFolder(), archiveName).toFile();
      if (!archiveFolderPath.toFile().exists()) {
        if (!archiveFolderPath.toFile().mkdir()) {
          LOGGER.error("Could not create folder {}", archivePath);
          // archive folder create failed, so don't delete files.
          return;
        }
      }
    }
    
    int archiveCount = 0;
    int deleteCount = 0;
    
    // Process files
    var files = Paths.get(path).toFile().listFiles();
    for (var file : files) {
      if (file.isFile() && !file.isHidden()) {
        if (fileNameMatch(file, config)) {
          // File pattern matches; check file age
          long fileAge = fileAgeDays(file);
          if (fileAge > config.getFileAgeDays()) {
            if (archivePath != null) {
              if (addFileToZip(file, archivePath)) {
                ++archiveCount;
                LOGGER.info("Archived file: {}", file.getName());
              }
              else {
                // archive failed; do not delete file
                continue;
              }
            }
            
            LOGGER.info("Delete file {}", file.getName());
            if (file.delete()) {
              ++deleteCount;
            }
            else {
              LOGGER.error("Failed to delete {}", file);
            }
          }
        }
      }
    }
    
    LOGGER.info("Archived {} files, deleted {} files", archiveCount, deleteCount);
 
  }
  
  /**
   * Returns true if all of the following are true.
   * <ul>
   * <li>File is not a properties file</li>
   * <li>File name matches the config pattern </li>
   * <li>File name is not the archive folder</li>
   * </ul>
   * @param file
   * @param config
   * @return 
   */
  static boolean fileNameMatch(File file, PathConfig config) {
    
    boolean nameMatch = file.getName().matches(config.getFilePattern());
    
    return file.isFile() &&
           !CONFIG_FILE_NAMES.contains(file.getName()) &&
           file.getName().compareToIgnoreCase(config.getArchiveFolder()) != 0 &&
           nameMatch;
  }
  
  /**
   * Adds the specified file to the specified zip file.
   * 
   * @param file
   * @param zipFile
   * @return
   * @throws IOException 
   */
  static boolean addFileToZip(File file, File zipFile) throws IOException {
    Map<String, String> env = new HashMap<String, String>();
    // check if file exists
    env.put("create", String.valueOf(!zipFile.exists()));
    
    // TODO: Assume it would be more efficient to create the file system
    // in the calling process, to avoid doing it lots of times
    
    try (FileSystem zipfs = FileSystems.newFileSystem(zipFile.toPath(), env)) {
      Path pathInZipFile = zipfs.getPath(file.getName());
      Files.copy(file.toPath(), pathInZipFile, StandardCopyOption.REPLACE_EXISTING);
      return true;
    }    
    catch (Exception ex) {
      LOGGER.error("Error adding {} to {}", file.getName(), zipFile.getName(), ex);
      return false;
    }
  }
  
  /**
   * Returns the age of a file, in days.
   * @param file
   * @return
   * @throws IOException 
   */
  static long fileAgeDays(File file) throws IOException {
    FileTime fileTime = Files.getLastModifiedTime(file.toPath());
    Instant fileInstant = fileTime.toInstant();
    Instant now = new Date().toInstant();
    Duration difference = Duration.between(fileInstant, now);
    return difference.toDays();    
  }
  
  
}
