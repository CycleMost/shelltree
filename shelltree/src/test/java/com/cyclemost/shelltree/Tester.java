package com.cyclemost.shelltree;

import org.apache.commons.cli.ParseException;
import org.junit.Test;

/**
 *
 * @author dbridges
 */
public class Tester {
 
  @Test
  public void testCommand() throws ParseException {
    ShellTree.main(new String[] {"-path", "/Users/dbridges/CycleMost/shelltree/shelltree/src/main/resources/testroot"});
  }
  
}
