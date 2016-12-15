package com.adaptris.maven.fingerprint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;

public class Utils {

  private static final String UTF_8 = "UTF-8";
  private static final String LINUX_EOL = "\n";
  private static final String EOL_PATTERN = "\\r\\n?";

  public static String generateMd5Fingerprint(File file) throws MojoExecutionException {
    if (file == null) {
      throw new MojoExecutionException("file should not be null to generate the Md5 Fingerprint");
    }
    String fingerprint;
    try {
      String fileContent = FileUtils.readFileToString(file, UTF_8);
      // Always linux format
      fileContent = fileContent.replaceAll(EOL_PATTERN, LINUX_EOL);
      fingerprint = DigestUtils.md5Hex(fileContent);
    } catch (Exception expt) {
      throw new MojoExecutionException(
          "unable to calculate md5 for file: " + file.getAbsolutePath(), expt);
    }
    return fingerprint;
  }

  public static String readFile(File file) throws MojoExecutionException {
    if (file == null) {
      throw new MojoExecutionException("file should not be null to be able to read it");
    }
    try {
      return FileUtils.readFileToString(file, UTF_8);
    } catch (Exception e) {
      throw new MojoExecutionException("unable to read file: " + file.getAbsolutePath(), e);
    }
  }

  public static void writeInFile(String content, File file) throws MojoExecutionException {
    if (file == null) {
      throw new MojoExecutionException("file should not be null to be able to write in it");
    }
    try {
      FileUtils.writeStringToFile(file, content, UTF_8);
    } catch (IOException e) {
      throw new MojoExecutionException("unable to write in file: " + file.getAbsolutePath(), e);
    }
  }

  public static void mkDirs(File srcDirectory, File destDirectory, Log log)
      throws MojoExecutionException {
    if (srcDirectory == null || destDirectory == null) {
      throw new MojoExecutionException("src and dest dir should not be null");
    }
    if (!srcDirectory.isDirectory()) {
      return;
    }
    File[] subFiles = srcDirectory.listFiles();
    for (File curFile : subFiles) {
      if (!curFile.isDirectory()) {
        continue;
      }
      File newDir = new File(destDirectory, curFile.getName());
      if (!newDir.exists() && !newDir.mkdirs()) {
        if (log != null) {
          log.warn("unable to create directory in outputDirectory: " + newDir);
        }
        continue;
      }
      mkDirs(curFile, newDir, log);
    }
  }

  public static String getFileExtension(String filename) {
    if (filename != null && !filename.equals("")) {
      int extensionIndex = filename.lastIndexOf(".");
      if (extensionIndex > -1) {
        return filename.substring(extensionIndex + 1);
      }
    }
    return null;
  }

  public static List<File> findFiles(File source, List<String> includes, List<String> excludes) {
    List<File> output = new ArrayList<>();
    if (source.isDirectory()) {
      DirectoryScanner scanner = new DirectoryScanner();

      scanner.setIncludes(includes.toArray(new String[includes.size()]));
      scanner.setExcludes(excludes.toArray(new String[excludes.size()]));
      scanner.addDefaultExcludes();
      scanner.setBasedir(source);
      scanner.scan();

      for (String includedFilename : scanner.getIncludedFiles()) {
        File curFile = new File(source, includedFilename);
        output.add(curFile);
      }
    }

    return output;
  }

}
