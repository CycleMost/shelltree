package com.cyclemost.shelltree;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dbridges
 */
public class ShellTree {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShellTree.class);

  private static final String CMD_PATH = "path";
  private static final String CMD_REPORT = "report";
  
  public static void main(String[] args) throws ParseException {

    // Create command line options
    Options options = new Options();
    
    Option processPathOption = Option.builder(CMD_PATH)
                         .argName("path [path]...")
                         .hasArgs()
                         .desc("root path(s) to process")
                         .build();    
    options.addOption(processPathOption);
    
    Option reportOnlyOption = Option.builder(CMD_REPORT)
                         .desc("run in report only mode")
                         .build();    
    options.addOption(reportOnlyOption);
    
    //parse the options passed as command line arguments
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    boolean reportOnly = false;
    if (cmd.hasOption(CMD_REPORT)) {
      reportOnly = true;
    }
    
    if (cmd.hasOption(CMD_PATH)) {
      String paths[] = cmd.getOptionValues(CMD_PATH);
      processPathCommand(paths, reportOnly);
    } else {
      printHelp(options);
    }
  }

  private static void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(160);
    formatter.printHelp(".", options);
  }
  
  private static void processPathCommand(String[] paths, boolean reportOnly) {
    if (paths == null || paths.length == 0) {
      LOGGER.info("No path specified");
      return;
    }
    
    for (String rootPath : paths) {
    
      LOGGER.debug("Starting at path: {}", rootPath);
      if (reportOnly) {
        LOGGER.info("Running in report-only mode; no changes will be made");
      }    

      ShellTreeProcessor processor = new ShellTreeProcessor(reportOnly);
      processor.process(rootPath, null);
      
      LOGGER.info("Complete. Archived {} files, deleted {} files, {}",  
        processor.getArchiveTotalCount(),
        processor.getDeleteTotalCount(),
        FileUtils.byteCountToDisplaySize(processor.getGrandTotalSize()));
      
    }
    
  }

}
