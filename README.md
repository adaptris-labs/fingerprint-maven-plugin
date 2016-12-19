fingerprint-maven-plugin
========================

Maven plugin for web resources optimization

About
---------------------

This plugin performs several optimizations:
  * Resource fingerprinting.
  * JS/CSS minification. yuicompressor is used
  * html minification.  
  
### Fingerprinting

During this process plugin calculates file checksum and prepends it to the file name. All links to this filename will be changed to the fingerprinted version. Original file will be deleted. Fingerprinting used to improve web resource caching. If file checksum is not changed, then the name will be the same and it is safe to add max expires header. Once file contents are changed, checksum will be changed as well. This plugin filters out (recursivly) source directory, detects any resources using the patterns below and copy result (if needed) to the target directory.

The following patterns are used to detect resources eligible for fingerprinting:
  * `<link.*?href="(.*?)".*?>`
  * `"([^\\s]*?\\.js)"`
  * `<img.*?src="(.*?)".*?>`
  * `url\("(.*?)"\)`
  * `<c:url.*?value=\")([/{1}|\\.{1,2}].*?)(\".*?>`
  * `fromUrl:\\s*[\",'])(.*?)([\",']`

After fingerprinting it is safe to add max expires header. 

Requirements

  * All resources should point to existing files without any pre-processing:
    * Valid: `<img src="/img/test.png">`
    * Invalid: `<img src="<c:if test="${var}">/img/test.png</c:if>"`

### HTML minification

During html minification:
  * all space between tags will be removed. Except `pre`.
  * `type="text"` will be removed from `input` tags since it's default type. 

Configuration
=============

  * Configure plugin in pom.xml:
  
```xml
			<plugin>
				<groupId>com.aerse.maven</groupId>
				<artifactId>fingerprint-maven-plugin</artifactId>
				<version>3.3</version>
				<executions>
					<execution>
						<phase>package</phase>
						<goals>
							<goal>generate</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<excludeResources>
						<excludeResource>://</excludeResource>
						<excludeResource>//</excludeResource>
						<excludeResource>data:</excludeResource>
					</excludeResources>
<!-- ${basedir}/src/main/webapp by default -->
					<sourceDirectory>${basedir}/target/webcombined</sourceDirectory>
<!-- ${project.build.directory}/optimized-webapp by default -->
					<targetDirectory>${basedir}/target/optimized-webapp</targetDirectory>
					<includes>
						<include>**/*.html</include>
						<include>**/*.jsp</include>
						<include>**/*.tag</include>
						<include>**/*.css</include>
						<include>**/*.js</include>
					</includes>
					<excludes>
						<exclude>toIgnore/**</exclude>
					</excludes>
<!-- cdn host. Not required. For example using "//accountname.r.worldssl.net": /css/bootstrap.css -> //accountname.r.worldssl.net/css/<md5>bootstrap.css -->
					<!-- Need to be (group1 before url)(group2 url)(group3 after url) -->
					<patterns>
						<pattern>(customUrlPattern=\\s*[\",'])(.*?)([\",'])</pattern>
					</patterns>
					<cdn>${cdn}</cdn>
				</configuration>
			</plugin>
```
  * Configure apache or nginx with max expires header. The following example is the configuration for nginx:

```xml
        location ~ ^/.+\.(ico|jpg|jpeg|gif|pdf|jar|png|js|css|txt|epf|ttf|svg|woff)$ {
            root         <your root content>;
            expires max;
            add_header Cache-Control public;
        }
```

