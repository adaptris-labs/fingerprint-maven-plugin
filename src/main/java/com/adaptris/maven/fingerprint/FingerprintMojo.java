package com.adaptris.maven.fingerprint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PACKAGE)
public class FingerprintMojo extends AbstractMojo {

  private static final String AMPERSAND = "&";
  private static final String QUESTION_MARK = "?";
  /*
   * Default patterns, more can be added using the patterns property
   */
  public Pattern LINK_PATTERN = Pattern.compile("(<link.*?href=\")(.*?)(\".*?>)");
  public Pattern SCRIPT_PATTERN = Pattern.compile("(\")([^\\s\"]*?\\.js)(\")");
  public Pattern IMG_PATTERN = Pattern.compile("(<img.*?src=\")([^\\}\\{]*?)(\".*?>)");
  public Pattern CSS_IMG_PATTERN = Pattern.compile("(url\\([\",'])(.*?)([\",']\\))");
  //  public Pattern JSTL_URL_PATTERN = Pattern.compile("(<c:url.*?value=\")(/{1}.*?)(\".*?>)");
  public Pattern JSTL_URL_PATTERN = Pattern.compile("(<c:url.*?value=\")([/{1}|\\.{1,2}].*?)(\".*?>)");
  public Pattern FROM_URL_PATTERN = Pattern.compile("(fromUrl:\\s*[\",'])(.*?)([\",'])");

  public Pattern DOLLAR_SIGN = Pattern.compile("\\$");

  public Set<Pattern> allPatterns = new HashSet<>();

  /**
   * target directory
   */
  @Parameter(defaultValue = "${project.build.directory}/optimized-webapp", required = true)
  private File targetDirectory;

  /**
   * Webapp directory
   */
  @Parameter(defaultValue = "${basedir}/src/main/webapp", required = true)
  private File sourceDirectory;

  /**
   * Exclude resources
   */
  @Parameter
  private List<String> excludeResources;

  @Parameter
  private List<String> extensionsToFilter;

  @Parameter
  private List<String> includeToFilters;

  @Parameter
  private List<String> excludeToFilters;

  @Parameter
  private Set<String> htmlExtensions;

  @Parameter
  private Set<String> patterns;

  /**
   * CDN url
   */
  @Parameter
  private String cdn;

  private final Set<String> processedFiles = new HashSet<>();
  private final Map<String, FilePathAndNewName> sourceToFingerprintedTarget = new HashMap<>();

  public FingerprintMojo() {
    allPatterns.add(LINK_PATTERN);
    allPatterns.add(SCRIPT_PATTERN);
    allPatterns.add(IMG_PATTERN);
    allPatterns.add(CSS_IMG_PATTERN);
    allPatterns.add(JSTL_URL_PATTERN);
    allPatterns.add(FROM_URL_PATTERN);
  }

  private void addPatterns() throws MojoExecutionException {
    if (patterns != null) {
      for (String pattern : patterns) {
        try {
          allPatterns.add(Pattern.compile(pattern));
        } catch (PatternSyntaxException pse) {
          throw new MojoExecutionException("unable to add custom pattern: " + pattern, pse);
        }
      }
    }
  }

  @Override
  public void execute() throws MojoExecutionException {
    addPatterns();

    if (!sourceDirectory.isDirectory()) {
      throw new MojoExecutionException("source directory is not a directory: " + sourceDirectory.getAbsolutePath());
    }
    if (!targetDirectory.exists()) {
      if (!targetDirectory.mkdirs()) {
        throw new MojoExecutionException("unable to create outputdirectory: " + targetDirectory.getAbsolutePath());
      }
    }
    if (!targetDirectory.isDirectory()) {
      throw new MojoExecutionException("output directory is not a directory: " + targetDirectory.getAbsolutePath());
    }
    if (extensionsToFilter == null || extensionsToFilter.isEmpty()) {
      getLog().info("no files to optimize found");
      return;
    }
//    if (includeToFilters == null || includeToFilters.isEmpty()) {
//      getLog().info("no files to optimize found");
//      return;
//    }
    List<File> filesToOptimize = new ArrayList<>();
    findFilesToOptimize(filesToOptimize, sourceDirectory);
    if (filesToOptimize.isEmpty()) {
      getLog().info("no files to optimize were found");
      return;
    }

    Utils.mkDirs(sourceDirectory, targetDirectory, getLog());

    for (File cur : filesToOptimize) {
      try {
        process(cur);
        processedFiles.add(cur.getAbsolutePath());
      } catch (Exception e) {
        getLog().error("unable to process: " + cur.getAbsolutePath(), e);
        throw new MojoExecutionException("unable to process: " + cur.getAbsolutePath(), e);
      }
    }

    copyDeepFiles(sourceDirectory, targetDirectory);

    // for (Entry<String, FilePathAndNewName> cur : sourceToFingerprintedTarget.entrySet()) {
    // String stripSourceDirectory;
    // stripSourceDirectory = stripSourceDirectory(sourceDirectory, new
    // File(cur.getValue().getFilePath()));
    // File toMove = new File(targetDirectory, stripSourceDirectory);
    // File dest = new File(targetDirectory, cur.getValue().getNewName());
    // // try {
    // // FileUtils.copyFile(toMove, dest);
    // // // FileUtils.forceDelete(toMove);
    // // // Files.move(toMove.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    // // } catch (IOException ioe) {
    // // throw new MojoExecutionException("unable to move src: " + toMove.getAbsolutePath() + "
    // dst:
    // // " + dest.getAbsolutePath(), ioe);
    // // }
    // // if (toMove.exists() && !toMove.renameTo(dest)) {
    // // throw new MojoExecutionException("unable to move src: " + toMove.getAbsolutePath() + "
    // dst: " + dest.getAbsolutePath());
    // // }
    // }
  }

  private void process(File sourceFile) throws MojoExecutionException {
    if (getLog().isDebugEnabled()) {
      getLog().debug("processing file: " + sourceFile.getAbsolutePath());
    }
    String data = Utils.readFile(sourceFile);
    StringBuffer outputFileData = new StringBuffer(data);
    for (Pattern pattern : allPatterns) {
      outputFileData = processPattern(pattern, outputFileData.toString(), sourceFile.getAbsolutePath());
    }
    String processedData = null;
    if (htmlExtensions != null && !htmlExtensions.isEmpty()) {
      String extension = Utils.getFileExtension(sourceFile.getName());
      if (extension != null && htmlExtensions.contains(extension)) {
        getLog().info("minifying html: " + sourceFile.getAbsolutePath());
        processedData = HtmlMinifier.minify(outputFileData.toString());
      }
    } else if (sourceFile.getName().contains(".min.")) {
      // getLog().info("ignoring already minified resource: " + sourceFile.getAbsolutePath());
    } else if (sourceFile.getName().endsWith(".js")) {
      // processedData = outputFileData.toString();
      // getLog().info("minifying javascript: " + sourceFile.getAbsolutePath());
      // processedData = Compressor.compressJavaScript(new StringReader(processedData), getLog());
    } else if (sourceFile.getName().endsWith(".css")) {
      // processedData = outputFileData.toString();
      // getLog().info("minifying css: " + sourceFile.getAbsolutePath());
      // processedData = Compressor.compressCSS(new StringReader(processedData), getLog());
    }

    if (processedData == null) {
      processedData = outputFileData.toString();
    }

    File targetFile = new File(targetDirectory, stripSourceDirectory(sourceDirectory, sourceFile));
    Utils.writeInFile(processedData, targetFile);
  }

  private StringBuffer processPattern(Pattern p, String data, String sourceOfData) throws MojoExecutionException {
    StringBuffer outputFileData = new StringBuffer();
    Matcher m = p.matcher(data);
    while (m.find()) {
      String curLink = m.group(2);
      if (getLog().isDebugEnabled()) {
        for (int i = 0; i < m.groupCount(); ++i) {
          getLog().debug("group " + i + ": " + m.group(i));
        }
      }
      if (isExcluded(curLink)) {
        getLog().info("resource excluded: " + curLink);
        m.appendReplacement(outputFileData, "$1" + curLink + "$3");
        continue;
      }
      int queryIndex = curLink.indexOf(QUESTION_MARK);
      String query = "";
      if (queryIndex != -1) {
        query = curLink.substring(queryIndex);
        curLink = curLink.substring(0, queryIndex);
      } else {
        queryIndex = curLink.indexOf("#");
        if (queryIndex != -1) {
          query = curLink.substring(queryIndex);
          curLink = curLink.substring(0, queryIndex);
        }
      }

      String targetPath = null;
      FilePathAndNewName filePathAndNewName = sourceToFingerprintedTarget.get(curLink);
      if (filePathAndNewName == null || filePathAndNewName.getNewName() == null) {
        // File curLinkFile = new File(sourceDirectory, curLink);
        String parent = new File(sourceOfData).getParent();
        File curLinkFile = new File(parent, curLink);
        if (!curLinkFile.exists()) {
          getLog().warn("resource file doesn't exist: " + curLink + " found in: " + sourceOfData);
          // escape dollar sign in result output
          curLink = DOLLAR_SIGN.matcher(curLink).replaceAll("\\\\\\$");
          m.appendReplacement(outputFileData, "$1" + curLink + "$3");
          continue;
        }
        // logIfRelativePath(curLink);
        String fingerprint = Utils.generateMd5Fingerprint(curLinkFile);
        targetPath = generateTargetResourceFilename(curLink, fingerprint);
        // logIfRelativePath(targetPath);

        try {
          filePathAndNewName = new FilePathAndNewName(curLinkFile.getCanonicalPath(),
              new File(targetPath).getName(), targetPath, fingerprint);
        } catch (IOException ioe) {
          throw new MojoExecutionException(
              "unable to get canonical path for: " + curLinkFile.getAbsolutePath(), ioe);
        }
        sourceToFingerprintedTarget.put(curLink, filePathAndNewName);
      } else {
        targetPath = filePathAndNewName.getNewPath();
      }

      String targetURL;
      int queryMarkIndex = query.indexOf(QUESTION_MARK);
      if (queryMarkIndex != -1 && targetPath.indexOf(QUESTION_MARK) != -1) {
        query = AMPERSAND + query.substring(queryMarkIndex + 1);
      }
      if (cdn == null) {
        targetURL = "$1" + targetPath + query + "$3";
      } else {
        targetURL = "$1" + cdn + targetPath + query + "$3";
      }

      m.appendReplacement(outputFileData, targetURL);
    }
    m.appendTail(outputFileData);
    return outputFileData;
  }

  // private void logIfRelativePath(String path) {
  // if (path.length() != 0 && path.charAt(0) != '/') {
  // getLog().warn("relative path detected: " + path);
  // }
  // }

  private boolean isExcluded(String path) {
    if (excludeResources != null) {
      for (String curExclude : excludeResources) {
        if (path.contains(curExclude)) {
          return true;
        }
      }
    }
    return false;
  }

  static String generateTargetResourceFilename(String sourceFilename, String fingerprint)
      throws MojoExecutionException {
    int index = sourceFilename.lastIndexOf("/");
    if (index == -1) {
      return sourceFilename + QUESTION_MARK + fingerprint;
    }
    String filename = sourceFilename.substring(index + 1);
    return sourceFilename.substring(0, index) + "/" + filename + QUESTION_MARK + fingerprint;
  }

  static String stripSourceDirectory(File sourceDirectory, File file) {
    return file.getAbsolutePath().substring(sourceDirectory.getAbsolutePath().length());
  }

  private void copyDeepFiles(File srcDir, File dstDir) throws MojoExecutionException {
    File[] srcFiles = srcDir.listFiles();
    for (File curFile : srcFiles) {
      if (curFile.isDirectory()) {
        copyDeepFiles(curFile, new File(dstDir, curFile.getName()));
        continue;
      }

      if (processedFiles.contains(curFile.getAbsolutePath())) {
        continue;
      }

      FileInputStream fis = null;
      FileOutputStream fos = null;
      try {
        fis = new FileInputStream(curFile);
        fos = new FileOutputStream(new File(dstDir, curFile.getName()));
        IOUtils.copy(fis, fos);
      } catch (Exception e) {
        throw new MojoExecutionException("unable to copy", e);
      }
    }
  }

  private void findFilesToOptimize(List<File> output, File source) {
    if (!source.isDirectory()) {
      return;
    }
    File[] subFiles = source.listFiles();
    for (File curFile : subFiles) {
      if (curFile.isDirectory()) {
        findFilesToOptimize(output, curFile);
        continue;
      }

      if (!curFile.isFile()) {
        continue;
      }

      String extension = Utils.getFileExtension(curFile.getName());
      if (extension == null) {
        continue;
      }

      if (extensionsToFilter.contains(extension)) {
        output.add(curFile);
        continue;
      }
//      if (matchIncludeToFilter(curFile.getName()) && !matchExcludeToFilter(curFile.getName())) {
//        output.add(curFile);
//        continue;
//      }
    }
  }

  private boolean matchIncludeToFilter(String link) {
    return match(link, includeToFilters);
  }

  private boolean matchExcludeToFilter(String link) {
    return match(link, excludeToFilters);
  }

  private boolean match(String link, List<String> filters) {
    if (link != null) {
      for (String filter : filters) {
        if (link.matches(filter)) {
          return true;
        }
      }
    }
    return false;
  }

  public class FilePathAndNewName {
    private final String filePath;
    private final String newName;
    private final String newPath;
    private final String fingerprint;

    public FilePathAndNewName(String filePath, String newName, String newPath, String fingerprint) {
      this.filePath = filePath;
      this.newName = newName;
      this.newPath = newPath;
      this.fingerprint = fingerprint;
    }

    public String getFilePath() {
      return filePath;
    }

    public String getNewName() {
      return newName;
    }

    public String getNewPath() {
      return newPath;
    }

    public String getFingerprint() {
      return fingerprint;
    }
  }

}
