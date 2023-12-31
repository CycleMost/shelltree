package com.cyclemost.shelltree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

/**
 * Holds values loaded from the .shelltree config file.
 * 
 * @author dbridges
 */
public class PathConfig {
  
  private String filePattern;
  private boolean recursive;
  private long fileAgeDays;
  private String archiveFolder;
  private long archiveAgeDays;
  
  /**
   * Creates an empty config.
   */
  public PathConfig() {
  }
  
  /**
   * Creates a new PathConfig object with values set by the 
   * specified Properties object.
   * @param properties 
   */
  public PathConfig(Properties properties) {
    loadProperties(properties);
  }
  
  /**
   * Creates a new PathConfig object with values set by the
   * specified properties file.
   * 
   * @param propertiesFile
   * @throws FileNotFoundException
   * @throws IOException 
   */
  public PathConfig(File propertiesFile) throws FileNotFoundException, IOException {
    Properties properties = new Properties();
    properties.load(new FileInputStream(propertiesFile));
    loadProperties(properties);
  }
  
  /**
   * Loads values from the specified Properties object.
   * @param properties 
   */
  public final void loadProperties(Properties properties) {
    setFilePattern(properties.getProperty("filePattern", "*"));
    setRecursive(Boolean.parseBoolean(properties.getProperty("recursive", "false")));
    setFileAgeDays(Integer.parseInt(properties.getProperty("fileAgeDays", "-1")));
    setArchiveFolder(properties.getProperty("archiveFolder", null));
    setArchiveAgeDays(Integer.parseInt(properties.getProperty("archiveAgeDays", "-1")));
  }
  
  @Override
  public String toString() {
    return String.format("filePattern: %s, recursive: %s, fileAgeDays: %s, archiveFolder: %s, archiveAgeDays: %s",
      getFilePattern(),
      isRecursive(),
      getFileAgeDays(),
      getArchiveFolder(),
      getArchiveAgeDays());
  }
  
  public boolean isFilePurgeEnabled() {
    return fileAgeDays > 0;
  }
  
  /**
   * Returns a flag indicating whether purged files in this folder
   * will be archived. 
   * 
   * @return 
   */
  public boolean isFileArchiveEnabled() {
    return StringUtils.isBlank(archiveFolder) && fileAgeDays > 0;
  }
  
  public List<String> getConfigWarnings() {
    List<String> warnings = new ArrayList<>();
    if (fileAgeDays < 1) {
      warnings.add("fileAgeDays not defined");
    }

    return warnings;
  }
  
  //// get/set methods

  public String getFilePattern() {
    return filePattern;
  }

  public void setFilePattern(String filePattern) {
    this.filePattern = filePattern;
  }

  public boolean isRecursive() {
    return recursive;
  }

  public void setRecursive(boolean recursive) {
    this.recursive = recursive;
  }

  public long getFileAgeDays() {
    return fileAgeDays;
  }

  public void setFileAgeDays(long fileAgeDays) {
    this.fileAgeDays = fileAgeDays;
  }

  public String getArchiveFolder() {
    return archiveFolder;
  }

  public void setArchiveFolder(String archiveFolder) {
    this.archiveFolder = archiveFolder;
  }

  public long getArchiveAgeDays() {
    return archiveAgeDays;
  }

  public void setArchiveAgeDays(long archiveAgeDays) {
    this.archiveAgeDays = archiveAgeDays;
  }  
}
