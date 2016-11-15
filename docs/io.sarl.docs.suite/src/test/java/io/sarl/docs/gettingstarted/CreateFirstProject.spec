/*
 * $Id$
 *
 * SARL is an general-purpose agent programming language.
 * More details on http://www.sarl.io
 *
 * Copyright (C) 2014-2016 the original authors or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sarl.docs.gettingstarted

import com.google.inject.Inject
import io.sarl.docs.utils.SARLParser
import io.sarl.docs.utils.SARLSpecCreator
import org.jnario.runner.CreateWith

import static extension io.sarl.docs.utils.SpecificationTools.*
import static extension org.junit.Assume.*
import io.sarl.lang.SARLConfig

/* @outline
 *
 * <p>For developing with SARL, you should create a project.
 * This document describes two ways for created SARL projects.
 *
 * <p>Two ways are available for creating a SARL project:
 * <ol>
 * <li>creating a SARL project inside Eclipse without Maven, or</li>
 * <li>creating a SARL project with Maven (inside or outside Eclipse).</li>
 * </ol>
 *
 * <p>These two ways are explained below.
 */
@CreateWith(SARLSpecCreator)
describe "Create First Project" {
	
	@Inject extension SARLParser

	/* For creating a project, you should open your Eclipse and click on
	 * **File > New > Projects**, and select *SARL Project* in
	 * the SARL category.
	 *
	 * <p>![Select the SARL Project Type](./new_sarl_project_screen_1.png)
	 *
	 *
	 * <p>After clicking on **Next**, the wizard is displaying the first page for creating a SARL project.
	 */
	describe "Create a SARL Project without Maven" {
		
		/* You must enter the name of your project. You could change the standard SARL and Java environment
		 * configurations as well.
		 * 
		 *
		 * <p>![Enter the Project Information](./new_sarl_project_screen_2.png)
		 *
		 *
		 * <p>Then you could click on **Next** for continuing the edition of the project's properties, or
		 * simply click on the **Finish** button for creating the project with the default properties.
		 * 
		 * 
		 * <p>The rest of this section is devoted to the edition of the additional properties for the SARL project.
		 * 
		 * @filter(.*) 
		 */
		fact "Step 1: Entering the project information" {
			"./new_sarl_project_screen_1.png" should beAccessibleFrom this
			"./new_sarl_project_screen_2.png" should beAccessibleFrom this
		}
		 
		/* The second page of the wizard contains the building settings.
		 * Two tabs are really interesting: the *Source* and the *Libraries*.
		 * 
		 * <p>The *Source* tab defines the folders in your project that must contains source code files.
		 * By default, a SARL project is composed of four source folders:
		 *
		 * * `src/main/java`: for your Java classes;
		 * * `src/main/sarl`: for your SARL scripts;
		 * * `src/main/generated-sources/sarl`: for the Java codes generated by the SARL compiler (you should not change them yourself);
		 * * `src/main/resources`: for the files that are not SARL nor Java code.
		 * 
		 * <p>The default output folder is `target/classes`.
		 * 
		 * <note>The names of these folders are following the
		 * conventions of a Maven-based project (described below). In this way, you will be able to 
		 * turn the Maven nature on your SARL project on/off.</note>
		 *
		 * <p>![Source Code Folders](./new_sarl_project_screen_3.png)
		 * 
		 * @filter(.*) 
		 */
		fact "Step 2: Configuration of the source folders" {
			SARLConfig::FOLDER_SOURCE_JAVA should be "src/main/java"
			SARLConfig::FOLDER_SOURCE_SARL should be "src/main/sarl"
			SARLConfig::FOLDER_SOURCE_GENERATED should be "src/main/generated-sources/sarl"
			SARLConfig::FOLDER_RESOURCES should be "src/main/resources"
			SARLConfig::FOLDER_BIN should be "target/classes"
			"./new_sarl_project_screen_3.png" should beAccessibleFrom this
		}
		 
	}

	/* For creating a project with both the Maven and SARL natures, you should open your 
	 * Eclipse and click on
	 * **File > New > Others > Maven > Maven Project**.
	 * 
	 * <p>Follow the steps of the project creation wizard, and finally click on the **Finish** button.
	 */
	describe "Create a SARL Project with Maven" {
		
		/* Open the file `pom.xml`, and edit it for obtaining a content similar to the
		 * configuration below.
		 * 
		 * <p>Replace the version number `%sarlversion%` of SARL
		 * with the one you want to use. You could search on the
		 * [Maven Central Repository](http://search.maven.org) for
		 * the last available version.
		 * The file [VERSION.txt](%sarlmavenrepository%/VERSION.txt)
		 * provides the latest version numbers of the SARL artifacts, as well.
		 * 
		 *     <project>
		 *        ...
		 *        <properties>
		 *           ...
		 *           <sarl.version>%sarlversion%</sarl.version>
		 *        </properties>
		 *        ...
		 *        <build>
		 *           <plugins>
		 *              ...
		 *              <plugin>
		 *                 <groupId>io.sarl.maven</groupId>
		 *                 <artifactId>sarl-maven-plugin</artifactId>
		 *                 <version>${sarl.version}</version>
		 *                 <extensions>true</extensions>
		 *                 <configuration>
		 *                    <source>%compilerlevel%</source>
		 *                    <target>%compilerlevel%</target>
		 *                    <encoding>%encoding%</encoding>
		 *                 </configuration>
		 *              </plugin>
		 *           </plugins>
		 *        </build>
		 *        ...
		 *        <dependencies>
		 *           ...
		 *           <dependency>
		 *              <groupId>io.sarl.maven</groupId>
		 *              <artifactId>io.sarl.maven.sdk</artifactId>
		 *              <version>${sarl.version}</version>
		 *           </dependency>
		 *           ...
		 *        </dependencies>
		 *        ...
		 *     </project>
		 * 
		 * <p>The Maven configuration is based on the use of `sarl-maven-plugin`.
		 * This plugin is in charge of compiling the SARL and the Java files.
		 * 
		 * <important>You must set the `extensions` tag to true for the 
		 * `sarl-maven-plugin` plugin. If you missed to set it, the plugin
		 * will not able to be integrated in the Maven life-cycle. The
		 * consequence will be that only the Java compiler will be invoked.</important>
		 * 
		 * @filter(.*) 
		 */
		fact "Edit the Maven configuration" {
			// Check if the SARL code is generated in the expected folder
			SARLConfig::FOLDER_SOURCE_GENERATED should be "src/main/generated-sources/sarl"
			// The checks are valid only if the macro replacements were done.
			// The replacements are done by Maven.
			// So, Eclipse Junit tools do not make the replacements.
			System.getProperty("sun.java.command", "").startsWith("org.eclipse.jdt.internal.junit.").assumeFalse
			// URLs should not end with a slash
			"%website%" should beURL "!file"
			"%sarlmavenrepository%" should beURL "!file"
		}
		
		/* For executing your SARL program, you must use a
		 * [runtime environment](%website%/runtime/index.html).
		 * 
		 * <p>The runtime environment that is recommended by the developers of SARL
		 * is [Janus](http://www.janusproject.io). 
		 * 
		 * <p>In several specific cases, you may want to include the runtime environment into the Maven dependencies
		 * of your project. In this case, you could replace the Maven dependency to the SARL sdk (as defined in
		 * the previous section) by a Maven dependency to the runtime environment.
		 * The
		 * 
		 * <p>Replace the version number (`%janusversion%`) of the [Janus platform](http://www.janusproject.io)
		 * with the one you want to use. You could search on the
		 * [Maven Central Repository](http://search.maven.org) for
		 * the last available version.
		 * The file [VERSION.txt](%janusmavenrepository%/VERSION.txt)
		 * provides the latest version numbers of the Janus artifacts, as well.
		 * 
		 * 
		 *     <project>
		 *        ...
		 *        <properties>
		 *           ...
		 *           <janus.version>%janusversion%</janus.version>
		 *        </properties>
		 *        ...
		 *        <build>
		 *           <plugins>
		 *              ...
		 *              <plugin>
		 *                 <groupId>io.sarl.maven</groupId>
		 *                 <artifactId>sarl-maven-plugin</artifactId>
		 *                 <version>${sarl.version}</version>
		 *                 <extensions>true</extensions>
		 *                 <configuration>
		 *                    <source>%compilerlevel%</source>
		 *                    <target>%compilerlevel%</target>
		 *                    <encoding>%encoding%</encoding>
		 *                 </configuration>
		 *              </plugin>
		 *           </plugins>
		 *        </build>
		 *        ...
		 *        <dependencies>
		 *           ...
		 *           <dependency>
		 *              <groupId>io.janusproject</groupId>
		 *              <artifactId>io.janusproject.kernel</artifactId>
		 *              <version>${janus.version}</version>
		 *           </dependency>
		 *           ...
		 *        </dependencies>
		 *        ...
		 *     </project>
		 * 
		 * <important>If you want to have the dependencies to both 
		 * `io.sarl.maven.sdk` and `io.janusproject.kernel` in your
		 * POM file, you must be sure that the imported version
		 * of the Google Guava library is the one provided by the Janus
		 * platform. For ensuring this, you must specify the version of the Guava
		 * library by defining it in the "dependencyManagement" section of your pom file.</important>
		 * 
		 * @filter(.*) 
		 */
		fact "Configuration of a runtime environment (optional)" {
			// The checks are valid only if the macro replacements were done.
			// The replacements are done by Maven.
			// So, Eclipse Junit tools do not make the replacements.
			System.getProperty("sun.java.command", "").startsWith("org.eclipse.jdt.internal.junit.").assumeFalse
			// URLs should not end with a slash
			"%website%" should beURL "!file"
			"%janusmavenrepository%" should beURL "!file"
		}
	} 
	
	/*
	 * In the next section, we will learn how to create our first agent.
	 * 
	 * <p>[Next>](AgentDefinitionIntroductionSpec.html)
	 * 
	 * @filter(.*)
	 */
	fact "What's next?" {
		"AgentDefinitionIntroductionSpec.html" should beAccessibleFrom this
	}

	/* Specification: SARL General-purpose Agent-Oriented Programming Language ("Specification")<br/>
	 * Version: %sarlspecversion%<br/>
	 * Status: %sarlspecreleasestatus%<br/>
	 * Release: %sarlspecreleasedate%
	 * 
	 * 
	 * <p>Copyright &copy; %copyrightdate% %copyrighters%.
	 * 
	 * <p>Licensed under the Apache License, Version 2.0;
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the [License](http://www.apache.org/licenses/LICENSE-2.0).
	 *
	 * @filter(.*) 
	 */
	fact "Legal Notice" {
		// The checks are valid only if the macro replacements were done.
		// The replacements are done by Maven.
		// So, Eclipse Junit tools do not make the replacements.
		System.getProperty("sun.java.command", "").startsWith("org.eclipse.jdt.internal.junit.").assumeFalse
		//
		"%sarlversion%" should startWith "%sarlspecversion%"
		("%sarlspecreleasestatus%" == "Stable Release"
			|| "%sarlspecreleasestatus%" == "Draft Release") should be true
		"%sarlspecreleasedate%" should beDate "YYYY-mm-dd"
		"%copyrightdate%" should beNumber "0000";
		("%copyrighters%".empty || "%copyrighters%".startsWith("%")) should be false
	}

}
