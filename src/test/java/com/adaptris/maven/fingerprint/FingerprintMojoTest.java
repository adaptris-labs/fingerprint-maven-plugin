package com.adaptris.maven.fingerprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Richard Scott Smith <scott.smith@isostech.com>
 */
public class FingerprintMojoTest {
  private static final String INPUT_DIR = "target/test-classes/to-parse";
  private static final String OUTPUT_DIR = "target/test-classes/fingerprinted";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private FingerprintMojo fingerprintMojo;

  @Before
  public void setUp() throws Exception {
    fingerprintMojo = new FingerprintMojo();

    // Configure the instance
    Class<FingerprintMojo> clazz = FingerprintMojo.class;

    configureInclude(fingerprintMojo, clazz);

    List<String> excludes = new ArrayList<>();
    excludes.add("ignore/**");
    Field excludesField = clazz.getDeclaredField("excludes");
    excludesField.setAccessible(true);
    excludesField.set(fingerprintMojo, excludes);

    List<String> excludeResources = new ArrayList<>();
    excludeResources.add("//");
    Field excludeResourcesField = clazz.getDeclaredField("excludeResources");
    excludeResourcesField.setAccessible(true);
    excludeResourcesField.set(fingerprintMojo, excludeResources);

    Set<String> patterns = new HashSet<>();
    patterns.add("(differentPatternUrl:\\s*[\",'])(.*?)([\",'])");
    Field patternsField = clazz.getDeclaredField("patterns");
    patternsField.setAccessible(true);
    patternsField.set(fingerprintMojo, patterns);

    configureSourceDir(fingerprintMojo, clazz);
    configureTargetDir(fingerprintMojo, clazz, OUTPUT_DIR);
  }

  private void configureSourceDir(FingerprintMojo fingerprintMojo, Class<FingerprintMojo> clazz)
      throws NoSuchFieldException, IllegalAccessException {
    File sourceDirectory = new File(INPUT_DIR);
    Field sourceDirectoryField = clazz.getDeclaredField("sourceDirectory");
    sourceDirectoryField.setAccessible(true);
    sourceDirectoryField.set(fingerprintMojo, sourceDirectory);
  }

  private void configureTargetDir(FingerprintMojo fingerprintMojo, Class<FingerprintMojo> clazz,
      String outputDir)
          throws NoSuchFieldException, IllegalAccessException {
    File targetDirectory = new File(outputDir);
    Field taretDirectoryField = clazz.getDeclaredField("targetDirectory");
    taretDirectoryField.setAccessible(true);
    taretDirectoryField.set(fingerprintMojo, targetDirectory);
  }

  private void configureInclude(FingerprintMojo fingerprintMojo, Class<FingerprintMojo> clazz)
      throws NoSuchFieldException, IllegalAccessException {
    List<String> includes = new ArrayList<>();
    includes.add("**/*.html");
    includes.add("**/*.css");
    includes.add("**/*.js");
    Field includesField = clazz.getDeclaredField("includes");
    includesField.setAccessible(true);
    includesField.set(fingerprintMojo, includes);
  }

  /**
   * Testing of the various regex patterns.
   * @throws Exception
   */
  @Test
  public void testPattern() throws Exception {
    Pattern linkPattern = FingerprintMojo.LINK_PATTERN;
    String linkUrl = "<link rel=\"stylesheet\" href=\"${pageContext.request.contextPath}/resources/css/style.css\" />";
    Matcher linkMatcher = linkPattern.matcher(linkUrl);
    assertTrue(linkMatcher.find());
    assertEquals("${pageContext.request.contextPath}/resources/css/style.css",
        linkMatcher.group(2));

    String linkUrl2 = "<link rel=\"stylesheet\" href=\"../resources/css/style.css\" />";
    Matcher linkMatcher2 = linkPattern.matcher(linkUrl2);
    assertTrue(linkMatcher2.find());
    assertEquals("../resources/css/style.css", linkMatcher2.group(2));

    Pattern scriptPattern = FingerprintMojo.SCRIPT_PATTERN;
    String scriptUrl = "<script src=\"${pageContext.request.contextPath}/resources/js/vendor/zepto.js\">";
    Matcher scriptMatcher = scriptPattern.matcher(scriptUrl);
    assertTrue(scriptMatcher.find());
    assertEquals("${pageContext.request.contextPath}/resources/js/vendor/zepto.js",
        scriptMatcher.group(2));

    String scriptUrl2 =
        "\"Something in quotes\"\"${pageContext.request.contextPath}/resources/js/vendor/zepto.js\"";
    Matcher scriptMatcher2 = scriptPattern.matcher(scriptUrl2);
    assertTrue(scriptMatcher2.find());
    assertEquals("${pageContext.request.contextPath}/resources/js/vendor/zepto.js",
        scriptMatcher2.group(2));

    Pattern imgPattern = FingerprintMojo.IMG_PATTERN;
    String imageUrl = "<img src=\"/images/favicon-whatever.ico\" />";
    Matcher imgMatcher = imgPattern.matcher(imageUrl);
    assertTrue(imgMatcher.find());
    assertEquals("/images/favicon-whatever.ico", imgMatcher.group(2));

    String imageUrl2 = "<img src=\"${pageContext.request.contextPath}/images/favicon-whatever.ico\" />";
    Matcher imgMatcher2 = imgPattern.matcher(imageUrl2);
    assertFalse(imgMatcher2.find());

    // Tests for the CSS image references
    Pattern cssPattern = FingerprintMojo.CSS_IMG_PATTERN;
    // Double quotes url, absolute location
    String cssUrl1 = "url(\"/images/navigation-s66728e073e.png\")";
    Matcher cssMatcher1 = cssPattern.matcher(cssUrl1);
    assertTrue(cssMatcher1.find());
    assertEquals("/images/navigation-s66728e073e.png", cssMatcher1.group(2));

    // Single quotes url, absolute location
    String cssUrl2 = "url('/images/navigation-s66728e073e.png')";
    Matcher cssMatcher2 = cssPattern.matcher(cssUrl2);
    assertTrue(cssMatcher2.find());
    assertEquals("/images/navigation-s66728e073e.png", cssMatcher2.group(2));

    // Double quotes url, relative location
    String cssUrl3 = "url(\"../images/navigation-s66728e073e.png\")";
    Matcher cssMatcher3 = cssPattern.matcher(cssUrl3);
    assertTrue(cssMatcher3.find());
    assertEquals("../images/navigation-s66728e073e.png", cssMatcher3.group(2));

    // Single quotes url, relative location
    String cssUrl4 = "url('../images/navigation-s66728e073e.png')";
    Matcher cssMatcher4 = cssPattern.matcher(cssUrl4);
    assertTrue(cssMatcher4.find());
    assertEquals("../images/navigation-s66728e073e.png", cssMatcher4.group(2));

    // JSTL url, absolute
    Pattern jstlUrlPattern = FingerprintMojo.JSTL_URL_PATTERN;
    String jstlUrl1 = "<c:url value=\"/resources/images/favicon.ico\" var=\"faviconUrl\"/>";
    Matcher jstlUrlMatcher1 = jstlUrlPattern.matcher(jstlUrl1);
    assertTrue(jstlUrlMatcher1.find());
    assertEquals("/resources/images/favicon.ico", jstlUrlMatcher1.group(2));

    // JSTL url, with context root
    String jstlUrl2 = "<c:url value=\"${pageContext.request.contextPath}/resources/images/favicon.ico\" var=\"faviconUrl\"/>";
    Matcher jstlUrlMatcher2 = jstlUrlPattern.matcher(jstlUrl2);
    assertFalse(jstlUrlMatcher2.find());

    // JSTL url, href
    String jstlUrl3 = "<c:url value=\"http://www.fedex.com/Tracking?ascend_header=1&amp;clienttype=dotcom&amp;cntry_code=us&amp;language=english&amp;tracknumbers=${shipment.trackingNumber}\" var=\"fedexUrl\"/>";
    Matcher jstlUrlMatcher3 = jstlUrlPattern.matcher(jstlUrl3);
    assertFalse(jstlUrlMatcher3.find());

    // Tests for the fromUrl for knockout js component
    Pattern fromUrlPattern = FingerprintMojo.FROM_URL_PATTERN;
    // Double quotes url, absolute location
    String fromUrl1 = "fromUrl: \"component.js\"";
    Matcher fromUrlMatcher1 = fromUrlPattern.matcher(fromUrl1);
    assertTrue(fromUrlMatcher1.find());
    assertEquals("component.js", fromUrlMatcher1.group(2));

    // Single quotes url, absolute location
    String fromUrl2 = "fromUrl: 'component.tmpl.html'";
    Matcher fromUrlMatcher2 = fromUrlPattern.matcher(fromUrl2);
    assertTrue(fromUrlMatcher2.find());
    assertEquals("component.tmpl.html", fromUrlMatcher2.group(2));

    // Double quotes url, relative location
    String fromUrl3 = "fromUrl: \"../component.js\"";
    Matcher fromUrlMatcher3 = fromUrlPattern.matcher(fromUrl3);
    assertTrue(fromUrlMatcher3.find());
    assertEquals("../component.js", fromUrlMatcher3.group(2));

    // Single quotes url, relative location
    String fromUrl4 = "fromUrl: '../component.tmpl.html'";
    Matcher fromUrlMatcher4 = fromUrlPattern.matcher(fromUrl4);
    assertTrue(fromUrlMatcher4.find());
    assertEquals("../component.tmpl.html", fromUrlMatcher4.group(2));
  }

  @Test
  public void testAddBadPattern() throws Exception {
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("unable to add custom patter");


    FingerprintMojo fingerprintMojo = new FingerprintMojo();

    // Configure the instance
    Class<FingerprintMojo> clazz = FingerprintMojo.class;

    Set<String> patterns = new HashSet<>();
    patterns.add("/a)(*");
    Field patternsField = clazz.getDeclaredField("patterns");
    patternsField.setAccessible(true);
    patternsField.set(fingerprintMojo, patterns);

    fingerprintMojo.execute();
  }

  @Test
  public void testSourceDirectoryNotDir() throws Exception {
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("source directory is not a directory");


    FingerprintMojo fingerprintMojo = new FingerprintMojo();

    // Configure the instance
    Class<FingerprintMojo> clazz = FingerprintMojo.class;

    File sourceDirectory = new File(INPUT_DIR + File.separator + "dummy-file-for-testing.html");
    Field sourceDirectoryField = clazz.getDeclaredField("sourceDirectory");
    sourceDirectoryField.setAccessible(true);
    sourceDirectoryField.set(fingerprintMojo, sourceDirectory);

    fingerprintMojo.execute();
  }

  @Test
  public void testTargetDirectoryNotDir() throws Exception {
    thrown.expect(MojoExecutionException.class);
    thrown.expectMessage("output directory is not a directory");

    FingerprintMojo fingerprintMojo = new FingerprintMojo();

    // Configure the instance
    Class<FingerprintMojo> clazz = FingerprintMojo.class;

    configureSourceDir(fingerprintMojo, clazz);

    File targetDirectory = new File(OUTPUT_DIR + File.separator + "dummy-file-for-testing.html");
    targetDirectory.getParentFile().mkdirs();
    targetDirectory.createNewFile();
    Field targetDirectoryField = clazz.getDeclaredField("targetDirectory");
    targetDirectoryField.setAccessible(true);
    targetDirectoryField.set(fingerprintMojo, targetDirectory);

    fingerprintMojo.execute();
  }

  @Test
  public void testNoInclude() throws Exception {
    File outputDirectory = new File(OUTPUT_DIR + "_testNoInclude");
    FingerprintMojo fingerprintMojo = new FingerprintMojo();

    // Configure the instance
    Class<FingerprintMojo> clazz = FingerprintMojo.class;

    configureSourceDir(fingerprintMojo, clazz);
    configureTargetDir(fingerprintMojo, clazz, outputDirectory.getAbsolutePath());

    FileUtils.deleteDirectory(outputDirectory);

    fingerprintMojo.execute();

    File dummyFileForTesting = new File(outputDirectory, "dummy-file-for-testing.html");
    assertFalse("file " + dummyFileForTesting.getAbsolutePath() + " should not exist", dummyFileForTesting.exists());
  }

  @Test
  public void testEmptyInclude() throws Exception {
    File outputDirectory = new File(OUTPUT_DIR + "_testEmptyInclude");
    FingerprintMojo fingerprintMojo = new FingerprintMojo();

    // Configure the instance
    Class<FingerprintMojo> clazz = FingerprintMojo.class;

    configureSourceDir(fingerprintMojo, clazz);
    configureTargetDir(fingerprintMojo, clazz, outputDirectory.getAbsolutePath());

    List<String> includes = new ArrayList<>();
    Field includesField = clazz.getDeclaredField("includes");
    includesField.setAccessible(true);
    includesField.set(fingerprintMojo, includes);

    FileUtils.deleteDirectory(outputDirectory);

    fingerprintMojo.execute();

    File dummyFileForTesting = new File(outputDirectory, "dummy-file-for-testing.html");
    assertFalse("file " + dummyFileForTesting.getAbsolutePath() + " should not exist",
        dummyFileForTesting.exists());
  }

  @Test
  public void testNoExclude() throws Exception {
    File outputDirectory = new File(OUTPUT_DIR + "_testNoExclude");
    FingerprintMojo fingerprintMojo = new FingerprintMojo();

    // Configure the instance
    Class<FingerprintMojo> clazz = FingerprintMojo.class;

    configureSourceDir(fingerprintMojo, clazz);
    configureTargetDir(fingerprintMojo, clazz, outputDirectory.getAbsolutePath());
    configureInclude(fingerprintMojo, clazz);

    FileUtils.deleteDirectory(outputDirectory);

    fingerprintMojo.execute();

    File dummyFileForTesting = new File(outputDirectory, "dummy-file-for-testing.html");
    assertTrue("file " + dummyFileForTesting.getAbsolutePath() + " should exist", dummyFileForTesting.exists());
  }

  @Test
  public void testEmptyExclude() throws Exception {
    File outputDirectory = new File(OUTPUT_DIR + "_testEmptyExclude");
    FingerprintMojo fingerprintMojo = new FingerprintMojo();

    // Configure the instance
    Class<FingerprintMojo> clazz = FingerprintMojo.class;

    configureSourceDir(fingerprintMojo, clazz);
    configureTargetDir(fingerprintMojo, clazz, outputDirectory.getAbsolutePath());
    configureInclude(fingerprintMojo, clazz);

    List<String> excludes = new ArrayList<>();
    Field excludesField = clazz.getDeclaredField("excludes");
    excludesField.setAccessible(true);
    excludesField.set(fingerprintMojo, excludes);

    FileUtils.deleteDirectory(outputDirectory);

    fingerprintMojo.execute();

    File dummyFileForTesting = new File(outputDirectory, "dummy-file-for-testing.html");
    assertTrue("file " + dummyFileForTesting.getAbsolutePath() + " should exist",
        dummyFileForTesting.exists());
  }

  @Test
  public void testExecute() throws Exception {
    File outputDirectory = new File(OUTPUT_DIR);
    FileUtils.deleteDirectory(outputDirectory);
    fingerprintMojo.execute();

    assertTrue(outputDirectory.exists());
    assertRootDummyFile(outputDirectory);
    assertSubDummyFile(outputDirectory);
    assertIgnoreDummyFile(outputDirectory);
    assertCssUrl(outputDirectory);
    assertJsUrl(outputDirectory);
  }

  private void assertRootDummyFile(File outputDirectory) throws MojoExecutionException {
    File dummyFileForTesting = new File(outputDirectory, "dummy-file-for-testing.html");
    assertTrue("file " + dummyFileForTesting.getAbsolutePath() + " should exist", dummyFileForTesting.exists());
    String fileContent = Utils.readFile(dummyFileForTesting);
    assertTrue(fileContent.contains("value=\"/favicon.ico?1a32b9900843d1339d968f1e36cb4930\""));
    assertTrue(fileContent.contains("href=\"css/style.css?0dde3d756e6a436c0b2ff85433038729\""));
    assertTrue(fileContent.contains("href=\"css/style.css?0dde3d756e6a436c0b2ff85433038729&param=value\""));
    assertTrue(fileContent.contains("href=\"css/style.css?0dde3d756e6a436c0b2ff85433038729#tag\""));
    assertTrue(fileContent.contains("href=\"css/doesntexist.css\""));
    assertTrue(fileContent.contains("src=\"//ajax.googleapis.com/ajax/libs/jquery/2.1.0/jquery.min.js\""));
    assertTrue(fileContent.contains("src=\"/js/script.js?f5039e9ac47dd3f0fbac0d6944e16561\""));
    assertTrue(fileContent.contains("src=\"./images/image.png?b0330d7d0b6ea9faccc7b93686e18230\""));
  }

  private void assertSubDummyFile(File outputDirectory) throws MojoExecutionException {
    File dummyFileForTesting = new File(outputDirectory, "sub/dummy-file-for-testing.html");
    assertTrue(dummyFileForTesting.exists());
    String fileContent = Utils.readFile(dummyFileForTesting);
    assertTrue(fileContent.contains("value=\"../favicon2.ico?1a32b9900843d1339d968f1e36cb4930\""));
    assertTrue(fileContent.contains("href=\"../css/style.css?0dde3d756e6a436c0b2ff85433038729\""));
    assertTrue(fileContent.contains("href=\"../css/style.css?0dde3d756e6a436c0b2ff85433038729&param=value\""));
    assertTrue(fileContent.contains("href=\"../css/style.css?0dde3d756e6a436c0b2ff85433038729#tag\""));
    assertTrue(fileContent.contains("href=\"../css/doesntexist.css\""));
    assertTrue(fileContent.contains("src=\"//ajax.googleapis.com/ajax/libs/jquery/2.1.0/jquery.min.js\""));
    assertTrue(fileContent.contains("src=\"../js/script.js?f5039e9ac47dd3f0fbac0d6944e16561\""));
    assertTrue(
        fileContent.contains("src=\"../images/image.png?b0330d7d0b6ea9faccc7b93686e18230\""));
  }

  private void assertIgnoreDummyFile(File outputDirectory) throws MojoExecutionException {
    File dummyFileForTesting = new File(outputDirectory, "ignore/dummy-file-for-testing.html");
    assertTrue(dummyFileForTesting.exists());
    String fileContent = Utils.readFile(dummyFileForTesting);
    assertTrue(fileContent.contains("value=\"../favicon2.ico\""));
    assertFalse(fileContent.contains("value=\"../favicon2.ico?1a32b9900843d1339d968f1e36cb4930\""));
    assertTrue(fileContent.contains("href=\"../css/style.css\""));
    assertFalse(fileContent.contains("href=\"../css/style.css?0dde3d756e6a436c0b2ff85433038729\""));
    assertTrue(fileContent.contains("href=\"../css/style.css?param=value\""));
    assertFalse(fileContent.contains("href=\"../css/style.css?0dde3d756e6a436c0b2ff85433038729&param=value\""));
    assertTrue(fileContent.contains("href=\"../css/style.css#tag\""));
    assertFalse(fileContent.contains("href=\"../css/style.css?0dde3d756e6a436c0b2ff85433038729#tag\""));
    assertTrue(fileContent.contains("href=\"../css/doesntexist.css\""));
    assertTrue(fileContent.contains("src=\"//ajax.googleapis.com/ajax/libs/jquery/2.1.0/jquery.min.js\""));
    assertTrue(fileContent.contains("src=\"../js/script.js\""));
    assertFalse(fileContent.contains("src=\"../js/script.js?f5039e9ac47dd3f0fbac0d6944e16561\""));
    assertTrue(fileContent.contains("src=\"../images/image.png\""));
    assertFalse(
        fileContent.contains("src=\"../images/image.png?b0330d7d0b6ea9faccc7b93686e18230\""));
  }

  private void assertCssUrl(File outputDirectory) throws MojoExecutionException {
    File cssFile = new File(outputDirectory, "css/style.css");
    assertTrue(cssFile.exists());
    String fileContent = Utils.readFile(cssFile);
    assertTrue(fileContent.contains("url(\"../images/image.png?b0330d7d0b6ea9faccc7b93686e18230\")"));
    assertTrue(fileContent.contains("url('../images/image.png?b0330d7d0b6ea9faccc7b93686e18230')"));
  }

  private void assertJsUrl(File outputDirectory) throws MojoExecutionException {
    File cssFile = new File(outputDirectory, "js/script.js");
    assertTrue(cssFile.exists());
    String fileContent = Utils.readFile(cssFile);
    assertTrue(fileContent.contains("fromUrl: 'component.js?29546d842fc6e36303afb9bf1b775377'"));
    assertTrue(fileContent.contains("fromUrl: \"component.tmpl.html?cb254044d90893fa918b79d272827fae\""));
    assertTrue(fileContent.contains("differentPatternUrl: 'component.js?29546d842fc6e36303afb9bf1b775377'"));
  }

  @Test
  public void testGenerateTargetFilename() throws Exception {
    File file = new File("src/test/resources/to-parse/dummy-file-for-testing.txt");
    File sourceDirectory = new File("src/test/resources/to-parse/");
    String targetHtmlFilename = FingerprintMojo.stripSourceDirectory(sourceDirectory, file);
    assertEquals(File.separator + "dummy-file-for-testing.txt", targetHtmlFilename);
  }

}
