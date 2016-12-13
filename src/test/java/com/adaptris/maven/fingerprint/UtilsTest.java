package com.adaptris.maven.fingerprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UtilsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGenerateMd5FingerprintNullSourceFile() throws MojoExecutionException {
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("file should not be null to generate the Md5 Fingerprint");

    Utils.generateMd5Fingerprint(null);
  }

  @Test
  public void testGenerateMd5FingerprintFileDoesntExist() throws MojoExecutionException {
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("unable to calculate md5 for file: ");

    Utils.generateMd5Fingerprint(new File("test.txt"));
  }

  @Test
  public void testGenerateMd5Fingerprint() throws MojoExecutionException, URISyntaxException {
    URL resource = getClass().getResource("/utils/utilsTestFile.txt");
    String m5Fingerprint = Utils.generateMd5Fingerprint(new File(resource.toURI()));
    assertEquals("e22229eaf7a54f4489d3d52d5ec21488", m5Fingerprint);
  }

  @Test
  public void testReadFileNullSourceFile() throws MojoExecutionException {
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("file should not be null to be able to read it");

    Utils.readFile(null);
  }

  @Test
  public void testReadFileFileDoesntExist() throws MojoExecutionException {
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("unable to read file: ");

    Utils.readFile(new File("test.txt"));
  }

  @Test
  public void testReadFile() throws MojoExecutionException, URISyntaxException {
    URL resource = getClass().getResource("/utils/utilsTestFile.txt");
    String fileContent = Utils.readFile(new File(resource.toURI()));
    assertEquals("Meaningless dummy file for Junit tests.", fileContent);
  }

  @Test
  public void testWriteFileNullSourceFile() throws MojoExecutionException {
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("file should not be null to be able to write in it");

    Utils.writeInFile("Some content", null);
  }

  @Test
  public void testWriteFileNullContene() throws MojoExecutionException, URISyntaxException {
    URL resource = getClass().getResource("/utils/utilsTestFile.txt");
    File newFile = new File(new File(resource.toURI()).getParentFile(), "newUtilsTestFile.txt");
    Utils.writeInFile(null, newFile);

    String fileContent = Utils.readFile(newFile);
    assertEquals("", fileContent);
  }

  @Test
  public void testWriteInFile() throws MojoExecutionException, URISyntaxException {
    URL resource = getClass().getResource("/utils/utilsTestFile.txt");
    File newFile = new File(new File(resource.toURI()).getParentFile(), "newUtilsTestFile.txt");
    Utils.writeInFile("Some content", newFile);

    String fileContent = Utils.readFile(newFile);
    assertEquals("Some content", fileContent);
  }

  @Test
  public void testMkDirsNullSourceDir() throws MojoExecutionException, URISyntaxException {
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("src and dest dir should not be null");

    URL resource = getClass().getResource("/utils/srcMkdirs");
    File srcDir = new File(resource.toURI());
    File destDir = new File(srcDir.getParentFile(), "destMkdirs");
    Utils.mkDirs(null, destDir, null);
  }

  @Test
  public void testMkDirsNullDestDir() throws MojoExecutionException, URISyntaxException {
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("src and dest dir should not be null");

    URL resource = getClass().getResource("/utils/srcMkdirs");
    File srcDir = new File(resource.toURI());
    Utils.mkDirs(srcDir, null, null);
  }

  @Test
  public void testMkDirs() throws MojoExecutionException, URISyntaxException, IOException {
    URL resource = getClass().getResource("/utils/srcMkdirs");
    File srcDir = new File(resource.toURI());
    File destDir = new File(srcDir.getParentFile(), "destMkdirs");
    if (destDir.exists()) {
      FileUtils.deleteDirectory(destDir);
    }
    assertFalse(destDir.exists());
    Utils.mkDirs(srcDir, destDir, null);

    assertTrue(destDir.exists());

    File subir1 = new File(destDir, "subDir1");
    assertTrue(subir1.exists());
    assertTrue(subir1.isDirectory());

    File subSubDir1 = new File(destDir, "subDir1/subSubDir1");
    assertTrue(subSubDir1.exists());
    assertTrue(subSubDir1.isDirectory());

    File subSubDir2 = new File(destDir, "subDir1/subSubDir2");
    assertTrue(subSubDir2.exists());
    assertTrue(subSubDir2.isDirectory());

    File subDir2 = new File(destDir, "subDir2");
    assertTrue(subDir2.exists());
    assertTrue(subDir2.isDirectory());
    assertTrue(new File(destDir, "subDir2/subSubDir1").exists());

    File subDirFile = new File(destDir, "subDir2/subDirFile");
    assertFalse(subDirFile.exists());
  }

  @Test
  public void testGetFileExtentionNullFilename() {
    assertNull(Utils.getFileExtension(null));
  }

  @Test
  public void testGetFileExtentionEmptyFilename() {
    assertNull(Utils.getFileExtension(""));
  }

  @Test
  public void testGetFileExtentionNoExtension() {
    assertNull(Utils.getFileExtension("filename"));
  }

  @Test
  public void testGetFileExtention() {
    assertEquals("txt", Utils.getFileExtension("filename.txt"));
  }

  @Test
  public void testGetFileExtentionTwoExtensions() {
    assertEquals("html", Utils.getFileExtension("filename.tmpl.html"));
  }

}
