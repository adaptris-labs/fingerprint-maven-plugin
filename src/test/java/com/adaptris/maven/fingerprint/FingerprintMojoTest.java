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
import org.junit.Test;

/**
 * @author Richard Scott Smith <scott.smith@isostech.com>
 */
public class FingerprintMojoTest {
  private static final String INPUT_DIR = "target/test-classes/to-parse";
  private static final String OUTPUT_DIR = "target/test-classes/fingerprinted";

  private FingerprintMojo fingerprintMojo;

  @Before
  public void setUp() throws Exception {
    fingerprintMojo = new FingerprintMojo();

    // Configure the instance
    Class<FingerprintMojo> clazz = FingerprintMojo.class;

    List<String> includes = new ArrayList<>();
    includes.add("**/*.html");
    includes.add("**/*.css");
    includes.add("**/*.js");
    Field includesField = clazz.getDeclaredField("includes");
    includesField.setAccessible(true);
    includesField.set(fingerprintMojo, includes);

    List<String> excludes = new ArrayList<>();
    excludes.add("ignore/**");
    Field excludesField = clazz.getDeclaredField("excludes");
    excludesField.setAccessible(true);
    excludesField.set(fingerprintMojo, excludes);

    List<String> excludeResources = new ArrayList<>();
    excludeResources.add("/js/libs");
    Field excludeResourcesField = clazz.getDeclaredField("excludeResources");
    excludeResourcesField.setAccessible(true);
    excludeResourcesField.set(fingerprintMojo, excludeResources);

    Set<String> patterns = new HashSet<>();
    patterns.add("(differentPatternUrl:\\s*[\",'])(.*?)([\",'])");
    Field patternsField = clazz.getDeclaredField("patterns");
    patternsField.setAccessible(true);
    patternsField.set(fingerprintMojo, patterns);

    // File sourceDirectory = new File("target/test-classes/to-parse");
    File sourceDirectory = new File(INPUT_DIR);
    Field sourceDirectoryField = clazz.getDeclaredField("sourceDirectory");
    sourceDirectoryField.setAccessible(true);
    sourceDirectoryField.set(fingerprintMojo, sourceDirectory);

    File outputDirectory = new File(OUTPUT_DIR);
    Field outputDirectoryField = clazz.getDeclaredField("targetDirectory");
    outputDirectoryField.setAccessible(true);
    outputDirectoryField.set(fingerprintMojo, outputDirectory);
  }

  /**
   * Testing of the various regex patterns.
   * @throws Exception
   */
  @Test
  public void testPattern() throws Exception {
    Pattern linkPattern = fingerprintMojo.LINK_PATTERN;
    String linkUrl = "<link rel=\"stylesheet\" href=\"${pageContext.request.contextPath}/resources/css/style.css\" />";
    Matcher linkMatcher = linkPattern.matcher(linkUrl);
    assertTrue(linkMatcher.find());
    assertEquals("${pageContext.request.contextPath}/resources/css/style.css",
        linkMatcher.group(2));

    String linkUrl2 = "<link rel=\"stylesheet\" href=\"../resources/css/style.css\" />";
    Matcher linkMatcher2 = linkPattern.matcher(linkUrl2);
    assertTrue(linkMatcher2.find());
    assertEquals("../resources/css/style.css", linkMatcher2.group(2));

    Pattern scriptPattern = fingerprintMojo.SCRIPT_PATTERN;
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

    Pattern imgPattern = fingerprintMojo.IMG_PATTERN;
    String imageUrl = "<img src=\"/images/favicon-whatever.ico\" />";
    Matcher imgMatcher = imgPattern.matcher(imageUrl);
    assertTrue(imgMatcher.find());
    assertEquals("/images/favicon-whatever.ico", imgMatcher.group(2));

    String imageUrl2 = "<img src=\"${pageContext.request.contextPath}/images/favicon-whatever.ico\" />";
    Matcher imgMatcher2 = imgPattern.matcher(imageUrl2);
    assertFalse(imgMatcher2.find());

    // Tests for the CSS image references
    Pattern cssPattern = fingerprintMojo.CSS_IMG_PATTERN;
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
    Pattern jstlUrlPattern = fingerprintMojo.JSTL_URL_PATTERN;
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
    Pattern fromUrlPattern = fingerprintMojo.FROM_URL_PATTERN;
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
    assertTrue(fileContent.contains("value=\"/favicon.ico?4a274f22d0754eef0654bf9448a999b3\""));
    assertTrue(fileContent.contains("href=\"css/style.css?8dcfc23d6a370ca167a39ebe905cc423\""));
    assertTrue(fileContent.contains("href=\"css/style.css?8dcfc23d6a370ca167a39ebe905cc423&param=value\""));
    assertTrue(fileContent.contains("href=\"css/style.css?8dcfc23d6a370ca167a39ebe905cc423#tag\""));
    assertTrue(fileContent.contains("src=\"/js/script.js?9994db6574d19a157f9b152ad8327ffd\""));
    assertTrue(fileContent.contains("src=\"./images/image.png?fce3b44b7d049d5be06eabad332cc3bf\""));
  }

  private void assertSubDummyFile(File outputDirectory) throws MojoExecutionException {
    File dummyFileForTesting = new File(outputDirectory, "sub/dummy-file-for-testing.html");
    assertTrue(dummyFileForTesting.exists());
    String fileContent = Utils.readFile(dummyFileForTesting);
    assertTrue(fileContent.contains("value=\"../favicon2.ico?4a274f22d0754eef0654bf9448a999b3\""));
    assertTrue(fileContent.contains("href=\"../css/style.css?8dcfc23d6a370ca167a39ebe905cc423\""));
    assertTrue(fileContent.contains("href=\"../css/style.css?8dcfc23d6a370ca167a39ebe905cc423&param=value\""));
    assertTrue(fileContent.contains("href=\"../css/style.css?8dcfc23d6a370ca167a39ebe905cc423#tag\""));
    assertTrue(fileContent.contains("src=\"../js/script.js?9994db6574d19a157f9b152ad8327ffd\""));
    assertTrue(fileContent.contains("src=\"../images/image.png?fce3b44b7d049d5be06eabad332cc3bf\""));
  }

  private void assertIgnoreDummyFile(File outputDirectory) throws MojoExecutionException {
    File dummyFileForTesting = new File(outputDirectory, "ignore/dummy-file-for-testing.html");
    assertTrue(dummyFileForTesting.exists());
    String fileContent = Utils.readFile(dummyFileForTesting);
    assertTrue(fileContent.contains("value=\"../favicon2.ico\""));
    assertFalse(fileContent.contains("value=\"../favicon2.ico?4a274f22d0754eef0654bf9448a999b3\""));
    assertTrue(fileContent.contains("href=\"../css/style.css\""));
    assertFalse(fileContent.contains("href=\"../css/style.css?8dcfc23d6a370ca167a39ebe905cc423\""));
    assertTrue(fileContent.contains("href=\"../css/style.css?param=value\""));
    assertFalse(fileContent.contains("href=\"../css/style.css?8dcfc23d6a370ca167a39ebe905cc423&param=value\""));
    assertTrue(fileContent.contains("href=\"../css/style.css#tag\""));
    assertFalse(fileContent.contains("href=\"../css/style.css?8dcfc23d6a370ca167a39ebe905cc423#tag\""));
    assertTrue(fileContent.contains("src=\"../js/script.js\""));
    assertFalse(fileContent.contains("src=\"../js/script.js?9994db6574d19a157f9b152ad8327ffd\""));
    assertTrue(fileContent.contains("src=\"../images/image.png\""));
    assertFalse(fileContent.contains("src=\"../images/image.png?fce3b44b7d049d5be06eabad332cc3bf\""));
  }

  private void assertCssUrl(File outputDirectory) throws MojoExecutionException {
    File cssFile = new File(outputDirectory, "css/style.css");
    assertTrue(cssFile.exists());
    String fileContent = Utils.readFile(cssFile);
    assertTrue(fileContent.contains("url(\"../images/image.png?fce3b44b7d049d5be06eabad332cc3bf\")"));
    assertTrue(fileContent.contains("url('../images/image.png?fce3b44b7d049d5be06eabad332cc3bf')"));
  }

  private void assertJsUrl(File outputDirectory) throws MojoExecutionException {
    File cssFile = new File(outputDirectory, "js/script.js");
    assertTrue(cssFile.exists());
    String fileContent = Utils.readFile(cssFile);
    assertTrue(fileContent.contains("fromUrl: 'component.js?1e819d866dd3d2b3a70d86d15316659a'"));
    assertTrue(fileContent.contains("fromUrl: \"component.tmpl.html?cb254044d90893fa918b79d272827fae\""));
    assertTrue(fileContent.contains("differentPatternUrl: 'component.js?1e819d866dd3d2b3a70d86d15316659a'"));
  }

  @Test
  public void testGenerateTargetFilename() throws Exception {
    File file = new File("src/test/resources/to-parse/dummy-file-for-testing.txt");
    File sourceDirectory = new File("src/test/resources/to-parse/");
    String targetHtmlFilename = FingerprintMojo.stripSourceDirectory(sourceDirectory, file);
    assertEquals(File.separator + "dummy-file-for-testing.txt", targetHtmlFilename);
  }

}
