package com.cyclemost.shelltree;

import java.io.File;
import java.io.FileFilter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author dbridges
 */
public class Tester {
 
  @Test
  @Ignore
  public void testCommand() throws ParseException {
    //ShellTree.main(new String[] {});
    ShellTree.main(new String[] {
      "-report", 
      "-path", 
      "/Users/dbridges/CycleMost/shelltree/shelltree/src/main/resources/testroot"
    });
  }
  
  @Test
  @Ignore
  public void testWildcards() {

    String fileNames[] = {"test.csv", "test.txt", "test.dat", "stuff.log"};
    
    String filters[] = {"*"};
    
    FileFilter fileFilter = new WildcardFileFilter(filters);

    for (var fileName : fileNames) {
      System.out.println(String.format("%s: %s", fileName, fileFilter.accept(new File(fileName))));
    }

  }
}
  

