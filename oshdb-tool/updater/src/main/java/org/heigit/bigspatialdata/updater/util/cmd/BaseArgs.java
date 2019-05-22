package org.heigit.bigspatialdata.updater.util.cmd;

import com.beust.jcommander.Parameter;
import java.nio.file.Path;
import org.heigit.bigspatialdata.oshdb.tool.importer.cli.validator.DirExistValidator;

public class BaseArgs {

  @Parameter(names = {"-changeIndex", "-dbBit"},
      description = "jdbc-connection of change-bitmap, will be same as update-db if not specified",
      required = false,
      order = 2)
  public String dbbit = null;

  @Parameter(names = {"-help", "--help", "-h", "--h"},
      description = "prints this help",
      help = true,
      order = 99)
  public boolean help = false;

  @Parameter(names = {"-jdbc"},
      description = "Connection details for jdbc-storage of updates: jdbc:dbms://host:port/database?user=UserName&password=Password",
      required = true,
      order = 1)
  public String jdbc;

  @Parameter(names = {"-etl"},
      description = "Path to etlFiles",
      validateWith = DirExistValidator.class,
      required = true,
      order = 2)
  public Path etl;

  @Parameter(names = {"-batchSize"},
      description = "number of Changes or Entities to be processed at once",
      required = false,
      order = 5)
  public int batchSize = 1000;

}
