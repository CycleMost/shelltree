package com.cyclemost.shelltree;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dbridges
 */
public class ShellTree {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShellTree.class);

  public static void main(String[] args) throws ParseException {

    // Create command line options
    Options options = new Options();
    
    Option processPath = Option.builder("path")
                         .argName("path")
                         .hasArg()
                         .desc("root path to process")
                         .build();    
    options.addOption(processPath);
    
    //parse the options passed as command line arguments
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    if (cmd.hasOption("path")) {
      String values[] = cmd.getOptionValues("path");
      processPathCommand(values);
    } else {
      printHelp(options);
    }
  }

  private static void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(160);
    formatter.printHelp(".", options);
  }
  
  private static void processPathCommand(String[] values) {
    if (values == null || values.length == 0) {
      LOGGER.info("No path specified");
      return;
    }
    
    String rootPath = values[0];
    
    LOGGER.debug("Starting at path: {}", rootPath);
    ShellTreeProcessor.process(rootPath, null);
    
  }

}
