package com.cyclemost.shelltree;

import java.io.File;
import java.io.FileFilter;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main logic for processing directories.
 * 
 * @author dbridges
 */
public class ShellTreeProcessor {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ShellTreeProcessor.class);
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HHmmss"); 
  public static List<String> CONFIG_FILE_NAMES = Arrays.asList(new String[] {"shelltree.properties", ".shelltree"});

  boolean reportOnly;
  boolean pruneArchive;
  
  int archiveTotalCount = 0;
  int deleteTotalCount = 0;
  long grandTotalSize = 0;
  int totalPathsProcessed = 0;
    
  public ShellTreeProcessor() {
  }
  
  /**
   * Processes the specified path. The entire directory tree under the
   * specified path will be processed, recursively.
   * 
   * @param path root path to begin processing
   * @param parentConfig options of parent folder, or null for none
   */
  public void process(String path, PathConfig parentConfig) {
    
    ++totalPathsProcessed;
    
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
        // TODO: cascade parent values?
        config = new PathConfig(configFile);
      }
      else {
        // no config file in this folder; use parent config
        config = parentConfig;
      }
      
      // If we have config, perform actions on this path
      if (config != null) {
        performActions(path, config);
      }
      else {
        //LOGGER.debug("no config for {}", path);
        config = new PathConfig();
      }
      
      // Process subfolders
      var files = Paths.get(path).toFile().listFiles();
      for (var file : files) {
        if (isValidDirectory(file, config)) {
          process(file.getAbsolutePath(), 
                  config.isRecursive() ? config : null);
        }
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
  void performActions(String path, PathConfig config) throws IOException {
    
    LOGGER.info( "Processing: {}", path);
    LOGGER.debug("Config: {}", config);
        
    int archiveCount = 0;
    int deleteCount = 0;
    long totalSize = 0;
    
    String filters[] = StringUtils.split(config.getFilePattern(), ";");
    FileFilter fileFilter = new WildcardFileFilter(filters);
        
    // Get list of files to be purged
    List<File> filesToPurge = new ArrayList<>();
    var files = Paths.get(path).toFile().listFiles();
    for (var file : files) {
      if (fileNameMatch(file, fileFilter, config)) {
        // File pattern matches; check file age
        long fileAge = fileAgeDays(file);
        if (fileAge > config.getFileAgeDays() && config.getFileAgeDays() > 0) {
          LOGGER.info("Delete file {} ({} days old)", file.getName(), fileAge);
          filesToPurge.add(file);
        }
      }
    }

    if (reportOnly) {
      return;
    }
    
    // Archive files
    Path archiveFolderPath = Paths.get(path, config.getArchiveFolder());
    if (!filesToPurge.isEmpty() && config.isFileArchiveEnabled()) {
      String archiveName = String.format("archive-%s.zip", DATE_FORMAT.format(new Date()));
      File archivePath = Paths.get(path, config.getArchiveFolder(), archiveName).toFile();
      if (!archiveFolderPath.toFile().exists()) {
        if (!archiveFolderPath.toFile().mkdir()) {
          LOGGER.error("Could not create folder {}", archiveFolderPath);
          // archive folder create failed, so don't delete files.
          return;
        }
      }  
      Map<String, String> env = new HashMap<>();
      env.put("create", String.valueOf(!archivePath.exists()));    
      try (FileSystem zipFileSystem = FileSystems.newFileSystem(archivePath.toPath(), env)) {
        for (File file : filesToPurge) {
          if (addFileToArchive(file, zipFileSystem)) {
            ++archiveCount;
            LOGGER.info("Archived file: {}", file.getName());
          }
          else {
            // archive failed; do not delete file
            LOGGER.error("Archive failed for {}", file.getName());
          }
        }
      }
    }
    
    // Delete files
    for (File file : filesToPurge) {
      try {
        long fileSize = FileUtils.sizeOf(file);
        Files.delete(file.toPath());
        ++deleteCount;
        totalSize += fileSize;      
      }
      catch (IOException ex) {
        LOGGER.error("Error deleting {}", file, ex);
      }
    }
        
    LOGGER.info("Archived {} files, deleted {} files, {}",  
      archiveCount, deleteCount, FileUtils.byteCountToDisplaySize(totalSize));
    
    archiveTotalCount += archiveCount;
    deleteTotalCount += deleteCount;
    grandTotalSize += totalSize;
    
    // Purge archives
    if (config.getArchiveAgeDays() > 0 && archiveFolderPath != null) {
      File archiveFolder = archiveFolderPath.toFile();
      if (archiveFolder.exists()) {
        for (File file : archiveFolder.listFiles()) {
          if (isArchiveFile(file)) {
            long fileAge = fileAgeDays(file);
            if (fileAge > config.getArchiveAgeDays()) {
              LOGGER.info("Delete archive file {} ({} days old)", file.getName(), fileAge);
              if (!reportOnly) {
                long fileSize = FileUtils.sizeOf(file);
                if (file.delete()) {
                  grandTotalSize += fileSize;
                }
              }
            }
          }
        }
        
        // If archive folder is empty, remove it
        if (pruneArchive) {
          boolean hasFiles = false;
          List<File> trash = new ArrayList<>();
          for (var file : archiveFolder.listFiles()) {
            if (file.isFile() && !file.isHidden()) {
              hasFiles = true;
              break;
            }
            trash.add(file);
          }
          if (!hasFiles) {
            try {
              // All remaining files are hidden ones. trash them.
              LOGGER.info("Removing empty archive folder {}", archiveFolder);
              for (var file : trash) {
                Files.delete(file.toPath());
              }
              Files.delete(archiveFolderPath);
            }
            catch (IOException ex) {
              LOGGER.error("Could not prune {}", archiveFolder, ex);
            }
          }
        }
        
      }
    }
    
  }
  
  /**
   * Returns true if this file appears to be an archive file.
   * 
   * @param file
   * @return 
   */
  private static boolean isArchiveFile(File file) {
    return !file.isHidden() && file.getName().toLowerCase().endsWith(".zip");
  }
  
  /**
   * Returns true if all of the following are true.
   * <ul>
   * <li>File is a directory</li>
   * <li>File is not hidden</li>
   * <li>File name is not the archive folder</li>
   * </ul>
   * 
   * @param file
   * @param config
   * @return 
   */
  static boolean isValidDirectory(File file, PathConfig config) {
    if (!file.isDirectory()) {
      return false;
    }
    if (file.isHidden()) {
      return false;
    }
    if (config.getArchiveFolder() != null) {
      if (file.getName().equalsIgnoreCase(config.getArchiveFolder())) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Returns true if all of the following are true.
   * <ul>
   * <li>File is not a properties file</li>
   * <li>File is not hidden</li>
   * <li>File name matches the config pattern </li>
   * </ul>
   * @param file
   * @param config
   * @return 
   */
  static boolean fileNameMatch(File file, FileFilter filter, PathConfig config) {
  
    boolean nameMatch = filter.accept(file);
    
    return nameMatch && 
           file.isFile() &&
           !file.isHidden() &&
           !CONFIG_FILE_NAMES.contains(file.getName());
  }
  
  /**
   * Adds the specified file to the zip archive.
   * 
   * @param file
   * @param zipFile
   * @return
   * @throws IOException 
   */
  static boolean addFileToArchive(File file, FileSystem zipFileSystem) throws IOException {
    try {
      Path pathInZipFile = zipFileSystem.getPath(file.getName());
      Files.copy(file.toPath(), pathInZipFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
      return true;
    }    
    catch (IOException ex) {
      LOGGER.error("Error archiving {}", file.getName(), ex);
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

  public boolean isReportOnly() {
    return reportOnly;
  }

  public void setReportOnly(boolean reportOnly) {
    this.reportOnly = reportOnly;
  }

  public int getArchiveTotalCount() {
    return archiveTotalCount;
  }

  public int getDeleteTotalCount() {
    return deleteTotalCount;
  }

  public long getGrandTotalSize() {
    return grandTotalSize;
  }

  public int getTotalPathsProcessed() {
    return totalPathsProcessed;
  }

  public boolean isPruneArchive() {
    return pruneArchive;
  }

  public void setPruneArchive(boolean pruneArchive) {
    this.pruneArchive = pruneArchive;
  }
  
}
