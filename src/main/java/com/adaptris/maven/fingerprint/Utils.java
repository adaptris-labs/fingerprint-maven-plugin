package com.adaptris.maven.fingerprint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class Utils {

  private static final String UTF_8 = "UTF-8";

  public static String generateMd5Fingerprint(File file) throws MojoExecutionException {
    if (file == null) {
      throw new MojoExecutionException("file should not be null to generate the Md5 Fingerprint");
    }
    String fingerprint;
    try {
      String fileContent = FileUtils.readFileToString(file, UTF_8);
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
    FileWriter w = null;
    try {
      w = new FileWriter(file);
      IOUtils.write(content, w);
    } catch (IOException e) {
      throw new MojoExecutionException("unable to write in file: " + file.getAbsolutePath(), e);
    } finally {
      IOUtils.closeQuietly(w);
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

}
