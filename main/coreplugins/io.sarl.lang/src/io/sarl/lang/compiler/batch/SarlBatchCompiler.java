/*
 * $Id$
 *
 * SARL is an general-purpose agent programming language.
 * More details on http://www.sarl.io
 *
 * Copyright (C) 2014-2018 the original authors or authors.
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

package io.sarl.lang.compiler.batch;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.inject.Provider;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtend.core.macro.ProcessorInstanceForJvmTypeProvider;
import org.eclipse.xtext.Constants;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.access.impl.ClasspathTypeProvider;
import org.eclipse.xtext.common.types.access.impl.IndexedJvmTypeAccess;
import org.eclipse.xtext.common.types.descriptions.IStubGenerator;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.generator.GeneratorContext;
import org.eclipse.xtext.generator.GeneratorDelegate;
import org.eclipse.xtext.generator.IFileSystemAccess;
import org.eclipse.xtext.generator.IOutputConfigurationProvider;
import org.eclipse.xtext.generator.JavaIoFileSystemAccess;
import org.eclipse.xtext.generator.OutputConfiguration;
import org.eclipse.xtext.generator.OutputConfigurationAdapter;
import org.eclipse.xtext.mwe.NameBasedFilter;
import org.eclipse.xtext.mwe.PathTraverser;
import org.eclipse.xtext.parser.IEncodingProvider;
import org.eclipse.xtext.resource.CompilerPhases;
import org.eclipse.xtext.resource.FileExtensionProvider;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.resource.persistence.StorageAwareResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.Files;
import org.eclipse.xtext.util.JavaVersion;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.util.UriUtil;
import org.eclipse.xtext.util.internal.AlternateJdkLoader;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.workspace.FileProjectConfig;
import org.eclipse.xtext.workspace.ProjectConfigAdapter;
import org.eclipse.xtext.xbase.compiler.GeneratorConfig;
import org.eclipse.xtext.xbase.compiler.GeneratorConfigProvider;
import org.eclipse.xtext.xbase.compiler.IGeneratorConfigProvider;
import org.eclipse.xtext.xbase.lib.Inline;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.resource.BatchLinkableResource;
import org.eclipse.xtext.xbase.resource.BatchLinkableResourceStorageWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.sarl.lang.SARLConfig;
import io.sarl.lang.compiler.GeneratorConfig2;
import io.sarl.lang.compiler.GeneratorConfigProvider2;
import io.sarl.lang.compiler.IGeneratorConfigProvider2;
import io.sarl.lang.compiler.batch.InternalXtextLogger.InternalXtextLoggerFactory;
import io.sarl.lang.extralanguage.IExtraLanguageContribution;
import io.sarl.lang.extralanguage.IExtraLanguageContributions;
import io.sarl.lang.util.Utils;
import io.sarl.lang.validation.IConfigurableIssueSeveritiesProvider;

/** The compiler from SARL that could be used for batch tasks (Maven, CLI).
 *
 * <p>This compiler is inspired by the Xtend batch compiler.
 *
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 * @since 0.5
 */
@SuppressWarnings({"checkstyle:classfanoutcomplexity", "checkstyle:methodcount", "checkstyle:classdataabstractioncoupling"})
public class SarlBatchCompiler {

	private static final String BINCLASS_FOLDER_PREFIX = "classes"; //$NON-NLS-1$

	private static final String STUB_FOLDER_PREFIX = "stubs"; //$NON-NLS-1$

	private static final String INTERNAL_ERROR_CODE = SarlBatchCompiler.class.getName() + ".internal_error"; //$NON-NLS-1$

	private static final FileFilter ACCEPT_ALL_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return true;
		}
	};

	private static final Predicate<IExtraLanguageContribution> DISABLER = it -> false;

	private static Class<? extends IJavaBatchCompiler> defaultJavaBatchCompiler;

	/** The provider of resource sets.
	 */
	protected Provider<ResourceSet> resourceSetProvider;

	private File outputPath;

	private File classOutputPath;

	private File tempPath;

	private boolean deleteTempPath = true;

	private List<File> bootClasspath;

	private List<File> classpath;

	private String encoding;

	private boolean writeTraceFiles = true;

	private boolean writeStorageFiles = true;

	private boolean verbose;

	private boolean enableJavaPostCompilation;

	private List<File> sourcePath;

	private boolean useCurrentClassLoaderAsParent;

	private org.eclipse.emf.common.util.URI baseUri;

	private FileProjectConfig projectConfig;

	private Map<String, OutputConfiguration> outputConfigurations;

	private ClassLoader currentClassLoader;

	private ClassLoader jvmTypesClassLoader;

	private ClassLoader annotationProcessingClassLoader;

	@Inject
	private IGeneratorConfigProvider generatorConfigProvider;

	@Inject
	private IGeneratorConfigProvider2 generatorConfigProvider2;

	@Inject
	private IOutputConfigurationProvider outputConfigurationProvider;

	@Inject
	private CompilerPhases compilerPhases;

	@Inject
	private Provider<JavaIoFileSystemAccess> javaIoFileSystemAccessProvider;

	@Inject
	private IndexedJvmTypeAccess indexedJvmTypeAccess;

	@Inject
	private IEncodingProvider.Runtime encodingProvider;

	@Inject
	private FileExtensionProvider fileExtensionProvider;

	@Inject
	private IResourceDescription.Manager resourceDescriptionManager;

	@Inject
	private IStubGenerator stubGenerator;

	@Inject
	private GeneratorDelegate generator;

	@Inject
	private IConfigurableIssueSeveritiesProvider issueSeverityProvider;

	@Inject
	private IExtraLanguageContributions extraLanguageContributions;

	@Inject
	@Named(Constants.LANGUAGE_NAME)
	private String languageName;

	private IJavaBatchCompiler javaCompiler;

	private Logger logger;

	private IssueMessageFormatter messageFormatter;

	private Collection<IssueMessageListener> messageListeners = new LinkedList<>();

	private Collection<ICompilatedResourceReceiver> resourceReceivers = new LinkedList<>();

	private final List<File> tempFolders = new ArrayList<>();

	private Comparator<Issue> issueComparator = new DefaultIssueComparator();

	private GeneratorConfig currentGeneratorConfiguration;

	private GeneratorConfig2 currentGeneratorConfiguration2;

	private String enabledExtraLanguageContributions;

	private boolean reportInternalProblemsAsIssues;

	private OptimizationLevel optimizationLevel;

	/** Constructor the batch compiler.
	 */
	public SarlBatchCompiler() {
		this.logger = LoggerFactory.getLogger(getClass());
	}

	/** Change the Java compiler.
	 *
	 * @param compiler the Java compiler
	 * @since 0.8
	 */
	@Inject
	public void setJavaCompiler(IJavaBatchCompiler compiler) {
		assert compiler != null;
		this.javaCompiler = compiler;
	}

	/** Create a default Java batch compiler, without injection.
	 *
	 * @return the Java batch compiler.
	 * @since 0.8
	 */
	public static IJavaBatchCompiler newDefaultJavaBatchCompiler() {
		try {
			synchronized (SarlBatchCompiler.class) {
				if (defaultJavaBatchCompiler == null) {
					final ImplementedBy annotation = IJavaBatchCompiler.class.getAnnotation(ImplementedBy.class);
					assert annotation != null;
					final Class<?> type = annotation.value();
					assert type != null;
					defaultJavaBatchCompiler = type.asSubclass(IJavaBatchCompiler.class);
				}
				return defaultJavaBatchCompiler.newInstance();
			}
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	/** Replies the Java compiler.
	 *
	 * @return the Java compiler
	 * @since 0.8
	 */
	public IJavaBatchCompiler getJavaCompiler() {
		if (this.javaCompiler == null) {
			this.javaCompiler = newDefaultJavaBatchCompiler();
		}
		return this.javaCompiler;
	}

	/** Change the optimization level that should be applied to the generated Java byte code.
	 *
	 * @param level the optimization level.
	 * @since 0.8
	 */
	public void setOptimizationLevel(OptimizationLevel level) {
		this.optimizationLevel = level;
	}

	/** Replies the optimization level that should be applied to the generated Java byte code.
	 *
	 * @return the optimization level.
	 * @since 0.8
	 */
	public OptimizationLevel getOptimizationLevel() {
		if (this.optimizationLevel == null) {
			this.optimizationLevel = OptimizationLevel.getDefault();
		}
		return this.optimizationLevel;
	}

	/** Change the flag that permits to report the compiler's internal problems as issues.
	 *
	 * @param reportAsIssues {@code true} if the internal errors are reported as issues.
	 * @since 0.8
	 * @see #addIssueMessageListener(IssueMessageListener)
	 */
	public void setReportInternalProblemsAsIssues(boolean reportAsIssues) {
		this.reportInternalProblemsAsIssues = reportAsIssues;
	}

	/** Replies the flag that indicates to report the compiler's internal problems as issues.
	 *
	 * @return {@code true} if the internal errors are reported as issues.
	 * @since 0.8
	 * @see #addIssueMessageListener(IssueMessageListener)
	 */
	public boolean getReportInternalProblemsAsIssues() {
		return this.reportInternalProblemsAsIssues;
	}

	/** Change the extra languages' generators that should be enabled.
	 *
	 * @param identifiers the identifier, the identifiers (separated by {@link File#pathSeparator} of the
	 *     extra languages' generator(s) to be enabled. If this parameter is {@code null}, all the extra
	 *     languages' generator are disabled.
	 * @since 0.8
	 */
	public void setExtraLanguageGenerators(String identifiers) {
		this.enabledExtraLanguageContributions = Strings.emptyIfNull(identifiers);
	}

	/** Replies the extra languages' generators that should be enabled.
	 *
	 * @return the identifier, the identifiers (separated by {@link File#pathSeparator} of the
	 *     extra languages' generator(s) to be enabled. If this parameter is {@code null}, all the extra
	 *     languages' generator are disabled.
	 * @since 0.8
	 */
	public String getExtraLanguageGenerators() {
		return this.enabledExtraLanguageContributions;
	}

	/** Set the comparator of issues that is used for sorting the issues before they are logged.
	 *
	 * @param comparator the comparator; never <code>null</code>.
	 */
	public void setIssueComparator(Comparator<Issue> comparator) {
		if (comparator != null) {
			this.issueComparator = comparator;
		}
	}

	/** Replies the comparator of issues that is used for sorting the issues before they are logged.
	 *
	 * @return the comparator; never <code>null</code>.
	 */
	public Comparator<Issue> getIssueComparator() {
		return this.issueComparator;
	}

	/** Replies if the Java compiler should be invoked after the SARL compiler is invoked.
	 *
	 * @return <code>true</code> if the Java compiler is invoked after the SARL compiler.
	 */
	public boolean isJavaPostCompilationEnable() {
		return this.enableJavaPostCompilation;
	}

	/** Set if the Java compiler should be invoked after the SARL compiler is invoked.
	 *
	 * @param enable <code>true</code> if the Java compiler is invoked after the SARL compiler.
	 */
	public void setJavaPostCompilationEnable(boolean enable) {
		this.enableJavaPostCompilation = enable;
	}

	/** Replies the formatter of the issue messages.
	 *
	 * @return the formatter.
	 */
	public IssueMessageFormatter getIssueMessageFormatter() {
		return this.messageFormatter;
	}

	/** Set the formatter of the issue messages.
	 *
	 * @param formatter the formatter.
	 */
	public void setIssueMessageFormatter(IssueMessageFormatter formatter) {
		this.messageFormatter = formatter;
	}

	/** Add a listener on the issue messages.
	 *
	 * @param listener the listener.
	 * @since 0.6
	 */
	public void addIssueMessageListener(IssueMessageListener listener) {
		this.messageListeners.add(listener);
	}

	/** Add a listener on the issue messages.
	 *
	 * @param listener the listener.
	 * @since 0.6
	 */
	public void removeIssueMessageListener(IssueMessageListener listener) {
		this.messageListeners.remove(listener);
	}

	/** Replies the message for the given issue.
	 *
	 * @param issue the issue.
	 * @param uri URI to the problem.
	 * @param message the formatted message.
	 * @since 0.6
	 */
	private void notifiesIssueMessageListeners(Issue issue, org.eclipse.emf.common.util.URI uri, String message) {
		for (final IssueMessageListener listener : this.messageListeners) {
			listener.onIssue(issue, uri, message);
		}
	}

	/** Add a receiver on the successfully compiled resources.
	 *
	 * @param receiver the receiver.
	 * @since 0.6
	 */
	public void addCompiledResourceReceiver(ICompilatedResourceReceiver receiver) {
		this.resourceReceivers.add(receiver);
	}

	/** Remove a receiver on the successfully compiled resources.
	 *
	 * @param receiver the receiver.
	 * @since 0.6
	 */
	public void removeCompiledResourceReceiver(ICompilatedResourceReceiver receiver) {
		this.resourceReceivers.remove(receiver);
	}

	/** Replies the message for the given issue.
	 *
	 * @param resource the compiled resource.
	 * @since 0.6
	 */
	private void notifiesCompiledResourceReceiver(Resource resource) {
		for (final ICompilatedResourceReceiver receiver : this.resourceReceivers) {
			receiver.receiveCompiledResource(resource);
		}
	}

	/** Replies the logger.
	 *
	 * @return the logger.
	 */
	public Logger getLogger() {
		return this.logger;
	}

	/** Set the logger.
	 *
	 * @param logger the logger.
	 */
	public void setLogger(Logger logger) {
		this.logger = logger == null ? LoggerFactory.getLogger(getClass()) : logger;
	}

	/** Set the provider of resource sets.
	 *
	 * @param resourceSetProvider the provider.
	 */
	@Inject
	public void setResourceSetProvider(Provider<ResourceSet> resourceSetProvider) {
		this.resourceSetProvider = resourceSetProvider;
	}

	private static File normalizeFile(String file) {
		return new File(new File(file).getAbsoluteFile().toURI().normalize());
	}

	/** Replies if the trace files must be generated.
	 *
	 * <p>A trace file contains the links between the class, java and SARL files.
	 * They are mandatory for retreiving and displaying the SARL source code from
	 * a JVM element.
	 *
	 * <p>The usual filename for the trace files follows the pattern
	 * {@code .Type.java._trace}, where {@code Type} is the name of the SARL type declaration.
	 *
	 * @return <code>true</code> for generation.
	 */
	public boolean isWriteTraceFiles() {
		return this.writeTraceFiles;
	}

	/** Set if the trace files must be generated.
	 *
	 * <p>A trace file contains the links between the class, java and SARL files.
	 * They are mandatory for retreiving and displaying the SARL source code from
	 * a JVM element.
	 *
	 * <p>The usual filename for the trace files follows the pattern
	 * {@code .Type.java._trace}, where {@code Type} is the name of the SARL type declaration.
	 *
	 * @param writeTraceFiles <code>true</code> for generation.
	 */
	public void setWriteTraceFiles(boolean writeTraceFiles) {
		this.writeTraceFiles = writeTraceFiles;
	}

	/** Replies if the storage files must be generated.
	 *
	 * <p>The storage files are binary versions of the resources in order
	 * to have faster reading/accessing.
	 *
	 * <p>The usual filename for the storage files follows the pattern
	 * {@code .Type.sarlbin}, where {@code Type} is the name of the SARL type declaration.
	 *
	 * @return <code>true</code> for generation.
	 */
	@Pure
	public boolean isWriteStorageFiles() {
		return this.writeStorageFiles;
	}

	/** Set if the storage files must be generated.
	 *
	 * <p>The storage files are binary versions of the resources in order
	 * to have faster reading/accessing.
	 *
	 * <p>The usual filename for the storage files follows the pattern
	 * {@code .Type.sarlbin}, where {@code Type} is the name of the SARL type declaration.
	 *
	 * @param writeStorageFiles <code>true</code> for generation.
	 */
	public void setWriteStorageFiles(boolean writeStorageFiles) {
		this.writeStorageFiles = writeStorageFiles;
	}

	/** Replies if the compiler is verbose.
	 *
	 * @return <code>true</code> if the compiler is verbose.
	 */
	@Pure
	public boolean isJavaCompilerVerbose() {
		return this.verbose;
	}

	/** Set the underlying Java compiler verbosity.
	 *
	 * @param verbose <code>true</code> if the Java compiler is verbose.
	 */
	public void setJavaCompilerVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/** Replies the current class loader.
	 *
	 * @return the class loader.
	 */
	@Pure
	public ClassLoader getCurrentClassLoader() {
		if (this.currentClassLoader == null) {
			this.currentClassLoader = getClass().getClassLoader();
		}
		return this.currentClassLoader;
	}

	/** Set the current class loader.
	 *
	 * @param loader the new current class loader.
	 */
	public void setCurrentClassLoader(ClassLoader loader) {
		this.currentClassLoader = null;
	}

	/** Set if the class loaderr of this batch compiler must be used as sthe parent class loader.
	 *
	 * @param useCurrentClassLoaderAsParent <code>true</code> for using the class loader of this batch compiler.
	 */
	public void setUseCurrentClassLoaderAsParent(boolean useCurrentClassLoaderAsParent) {
		this.useCurrentClassLoaderAsParent = useCurrentClassLoaderAsParent;
	}

	/** Replies if the class loaderr of this batch compiler must be used as sthe parent class loader.
	 *
	 * @return <code>true</code> for using the class loader of this batch compiler.
	 */
	@Pure
	public boolean isUseCurrentClassLoaderAsParent() {
		return this.useCurrentClassLoaderAsParent;
	}

	/** Change the base path.
	 *
	 * @param basePath the base path.
	 */
	public void setBasePath(String basePath) {
		setBaseURI(UriUtil.createFolderURI(normalizeFile(basePath)));
	}

	/** Change the base URI.
	 *
	 * @param basePath the base path.
	 */
	public void setBaseURI(org.eclipse.emf.common.util.URI basePath) {
		this.baseUri = basePath;
	}

	/** Change the path where the Java files are generated.
	 *
	 * @param path the path, or <code>null</code> for using the default path in {@link SARLConfig#FOLDER_SOURCE_GENERATED}..
	 */
	public void setOutputPath(File path) {
		this.outputPath = path;
	}

	/** Change the path where the Java files are generated.
	 *
	 * @param path the path.
	 */
	public void setOutputPath(String path) {
		setOutputPath(normalizeFile(path));
	}

	/** Replies the path where the Java files are generated.
	 *
	 * @return the path; or <code>null</code> for using the default path in {@link SARLConfig#FOLDER_SOURCE_GENERATED}.
	 */
	@Pure
	public File getOutputPath() {
		return this.outputPath;
	}

	/** Replies the path where the class files are generated.
	 *
	 * @return the path; or <code>null</code> for ignoring the class generation.
	 */
	@Pure
	public File getClassOutputPath() {
		return this.classOutputPath;
	}

	/** Set the path where the class files are generated.
	 *
	 * @param path the path; or <code>null</code> for ignoring the class generation.
	 */
	@Pure
	public void setClassOutputPath(File path) {
		this.classOutputPath = path;
	}

	/** Change the boot classpath.
	 *
	 * <p>The boot classpath is a list the names of folders or jar files that are separated by {@link File#pathSeparator}.
	 *
	 * @param bootClasspath the new boot classpath.
	 */
	public void setBootClassPath(String bootClasspath) {
		this.bootClasspath = new ArrayList<>();
		for (final String path : Strings.split(bootClasspath, Pattern.quote(File.pathSeparator))) {
			this.bootClasspath.add(normalizeFile(path));
		}
	}

	/** Change the boot classpath.
	 *
	 * @param bootClasspath the new boot classpath.
	 */
	public void setBootClassPath(Collection<File> bootClasspath) {
		this.bootClasspath = new ArrayList<>(bootClasspath);
	}

	/** Replies the boot classpath.
	 *@Inject
	private IResourceDescription.Manager resourceDescriptionManager;
	 * @return the boot classpath.
	 */
	@Pure
	public List<File> getBootClassPath() {
		if (this.bootClasspath == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(this.bootClasspath);
	}

	/** Change the classpath.
	 *
	 * <p>The classpath is a list the names of folders or jar files that are separated by {@link File#pathSeparator}.
	 *
	 * @param classpath the new classpath.
	 */
	public void setClassPath(String classpath) {
		this.classpath = new ArrayList<>();
		for (final String path : Strings.split(classpath, Pattern.quote(File.pathSeparator))) {
			this.classpath.add(normalizeFile(path));
		}
	}

	/** Change the classpath.
	 *
	 * @param classpath the new classpath.
	 */
	public void setClassPath(Collection<File> classpath) {
		this.classpath = new ArrayList<>(classpath);
	}

	/** Replies the classpath.
	 *
	 * @return the classpath.
	 */
	@Pure
	public List<File> getClassPath() {
		if (this.classpath == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(this.classpath);
	}

	/** Change the path where the Xtext stubs are generated.
	 *
	 * @param path the path.
	 */
	public void setTempDirectory(File path) {
		this.tempPath = path;
	}

	/** Change the path where the Xtext stubs are generated.
	 *
	 * @param path the path.
	 */
	public void setTempDirectory(String path) {
		setTempDirectory(normalizeFile(path));
	}

	/** Replies the path where the Xtext stubs are generated.
	 *
	 * @return the path; or <code>null</code> for using the default path.
	 */
	@Pure
	public File getTempDirectory() {
		if (this.tempPath == null) {
			this.tempPath = createTempDirectory();
		}
		return this.tempPath;
	}

	/** Create the temp directory that should be used by the compiler.
	 *
	 * @return the temp directory, never {@code null}.
	 */
	@SuppressWarnings("static-method")
	protected File createTempDirectory() {
		final File tmpPath = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		int i = 0;
		File tmp = new File(tmpPath, "sarlc" + i); //$NON-NLS-1$
		while (tmp.exists()) {
			++i;
			tmp = new File(tmpPath, "sarlc" + i); //$NON-NLS-1$
		}
		return tmp;
	}

	/** Replies if the temp folder must be deleted at the end of the compilation.
	 *
	 * @return <code>true</code> if the temp folder is deleted.
	 */
	@Pure
	public boolean isDeleteTempDirectory() {
		return this.deleteTempPath;
	}

	/** Set if the temp folder must be deleted at the end of the compilation.
	 *
	 * @param delete <code>true</code> if the temp folder is deleted.
	 */
	public void setDeleteTempDirectory(boolean delete) {
		this.deleteTempPath = delete;
	}

	/** Change the file encoding.
	 *
	 * @param encoding the encoding, usually <code>UTF-8</code>.
	 */
	public void setFileEncoding(String encoding) {
		this.encoding = encoding;
	}

	/** Change the file encoding.
	 *
	 * @return the file encoding, or <code>null</code> if the default encoding must be used.
	 */
	@Pure
	public String getFileEncoding() {
		return this.encoding;
	}

	/** Replies the current generator config.
	 *
	 * @return the generator config.
	 */
	protected GeneratorConfig getGeneratorConfig() {
		if (this.currentGeneratorConfiguration == null) {
			this.currentGeneratorConfiguration = this.generatorConfigProvider.get(null);
		}
		return this.currentGeneratorConfiguration;
	}

	/** Replies the current generator config v2.
	 *
	 * @return the generator config v2.
	 */
	protected GeneratorConfig2 getGeneratorConfig2() {
		if (this.currentGeneratorConfiguration2 == null) {
			this.currentGeneratorConfiguration2 = this.generatorConfigProvider2.get(null);
		}
		return this.currentGeneratorConfiguration2;
	}

	/** Change the version of the Java source to be used for the generated Java files.
	 *
	 * @param version the Java version.
	 */
	public void setJavaSourceVersion(String version) {
		final JavaVersion javaVersion = JavaVersion.fromQualifier(version);
		if (javaVersion == null) {
			final List<String> qualifiers = new ArrayList<>();
			for (final JavaVersion vers : JavaVersion.values()) {
				qualifiers.add(vers.getQualifier());
			}

			throw new RuntimeException(MessageFormat.format(
					Messages.SarlBatchCompiler_0, version, Joiner.on(Messages.SarlBatchCompiler_1).join(qualifiers)));
		}
		getGeneratorConfig().setJavaSourceVersion(javaVersion);
	}

	/** Replies the version of the Java source to be used for the generated Java files.
	 *
	 * @return the Java version.
	 */
	@Pure
	public String getJavaSourceVersion() {
		return getGeneratorConfig().getJavaSourceVersion().getQualifier();
	}

	/** Replies the compiler generate the Xbase expressions.
	 *
	 * @return <code>true</code> if the compiler generates the expressions
	 */
	@Pure
	public boolean isGenerateExpressions() {
		return getGeneratorConfig().isGenerateExpressions();
	}

	/** Set if the compiler generate the Xbase expressions.
	 *
	 * @param generateExpressions <code>true</code> if the compiler generates the expressions
	 */
	public void setGenerateExpressions(boolean generateExpressions) {
		getGeneratorConfig().setGenerateExpressions(generateExpressions);
	}

	/** Replies the <code>@SuppressWarnings</code> is generated.
	 *
	 * @return <code>true</code> if the compiler generates the warning supression annotations.
	 */
	@Pure
	public boolean isGenerateSyntheticSuppressWarnings() {
		return getGeneratorConfig().isGenerateSyntheticSuppressWarnings();
	}

	/** Set if the <code>@SuppressWarnings</code> is generated.
	 *
	 * @param generateAnnotations <code>true</code> if the compiler generates the warning supression annotations.
	 */
	public void setGenerateSyntheticSuppressWarnings(boolean generateAnnotations) {
		getGeneratorConfig().setGenerateSyntheticSuppressWarnings(generateAnnotations);
	}

	/** Replies the <code>@Generated</code> is generated.
	 *
	 * @return <code>true</code> if the compiler generates the generated annotations.
	 */
	@Pure
	public boolean isGenerateGeneratedAnnotation() {
		return getGeneratorConfig().isGenerateGeneratedAnnotation();
	}

	/** Set if the <code>@Generated</code> is generated.
	 *
	 * @param generateAnnotations <code>true</code> if the compiler generates the generated annotations.
	 */
	public void setGenerateGeneratedAnnotation(boolean generateAnnotations) {
		getGeneratorConfig().setGenerateGeneratedAnnotation(generateAnnotations);
	}

	/** Replies if the generation date is included in the <code>@Generated</code> annotations.
	 *
	 * @return <code>true</code> if the generation date is added.
	 */
	@Pure
	public boolean isIncludeDateInGeneratedAnnotation() {
		return getGeneratorConfig().isIncludeDateInGeneratedAnnotation();
	}

	/** Set if the generation date is included in the <code>@Generated</code> annotations.
	 *
	 * @param includeDateInGeneratedAnnotation <code>true</code> if the generation date is added.
	 */
	public void setIncludeDateInGeneratedAnnotation(boolean includeDateInGeneratedAnnotation) {
		getGeneratorConfig().setIncludeDateInGeneratedAnnotation(includeDateInGeneratedAnnotation);
	}

	/** Replies the comment in the <code>@Generated</code> annnotations.
	 *
	 * @return the comment.
	 */
	@Pure
	public String getGeneratedAnnotationComment() {
		return getGeneratorConfig().getGeneratedAnnotationComment();
	}

	/** Set the comment in the <code>@Generated</code> annnotations.
	 *
	 * @param comment the comment.
	 */
	public void setGeneratedAnnotationComment(String comment) {
		getGeneratorConfig().setGeneratedAnnotationComment(comment);
	}

	/** Replies if the <code>@Inline</code> shall be generated.
	 *
	 * @return <code>true</code> if annotation shall be generated.
	 */
	@Pure
	public boolean isGenerateInlineAnnotation() {
		return getGeneratorConfig2().isGenerateInlineAnnotation();
	}

	/** Set if the <code>@Inline</code> shall be generated.
	 *
	 * @param generateInlineAnnotation <code>true</code> if annotation shall be generated.
	 */
	public void setGenerateInlineAnnotation(final boolean generateInlineAnnotation) {
		getGeneratorConfig2().setGenerateInlineAnnotation(generateInlineAnnotation);
	}

	/** Replies if constant expression interpreter shall be called for generated <code>@Inline</code>.
	 *
	 * @return <code>true</code> if annotation shall be generated.
	 */
	@Pure
	public boolean isUseExpressionInterpreterForInlineAnnotation() {
		return getGeneratorConfig2().isUseExpressionInterpreterForInlineAnnotation();
	}

	/** Set if the constant expression interpreter shall be called for generated <code>@Inline</code>.
	 *
	 * @param generateInlineAnnotation <code>true</code> if annotation shall be generated.
	 */
	public void setUseExpressionInterpreterForInlineAnnotation(final boolean generateInlineAnnotation) {
		getGeneratorConfig2().setUseExpressionInterpreterForInlineAnnotation(generateInlineAnnotation);
	}

	/** Replies if the <code>@Pure</code> shall be generated.
	 *
	 * @return <code>true</code> if annotation shall be generated.
	 */
	@Pure
	public boolean isGeneratePureAnnotation() {
		return getGeneratorConfig2().isGeneratePureAnnotation();
	}

	/** Set if the <code>@Pure</code> shall be generated.
	 *
	 * @param generatePureAnnotation <code>true</code> if annotation shall be generated.
	 */
	public void setGeneratePureAnnotation(final boolean generatePureAnnotation) {
		getGeneratorConfig2().setGeneratePureAnnotation(generatePureAnnotation);
	}

	/** Replies if the equality test functions shall be generated.
	 *
	 * @return <code>true</code> if the functions shall be generated.
	 * @since 0.8
	 */
	@Pure
	public boolean isGenerateEqualityTestFunctions() {
		return getGeneratorConfig2().isGenerateEqualityTestFunctions();
	}

	/** Set if the equality test functions shall be generated.
	 *
	 * @param generateFunctions <code>true</code> if the functions shall be generated.
	 * @since 0.8
	 */
	public void setGenerateEqualityTestFunctions(final boolean generateFunctions) {
		getGeneratorConfig2().setGenerateEqualityTestFunctions(generateFunctions);
	}

	/** Replies if the toString functions shall be generated.
	 *
	 * @return <code>true</code> if the functions shall be generated.
	 * @since 0.8
	 */
	@Pure
	public boolean isGenerateToStringFunctions() {
		return getGeneratorConfig2().isGenerateToStringFunctions();
	}

	/** Set if the toString functions shall be generated.
	 *
	 * @param generateFunctions <code>true</code> if the functions shall be generated.
	 * @since 0.8
	 */
	public void setGenerateToStringFunctions(final boolean generateFunctions) {
		getGeneratorConfig2().setGenerateToStringFunctions(generateFunctions);
	}

	/** Replies if the clone functions shall be generated.
	 *
	 * @return <code>true</code> if the functions shall be generated.
	 * @since 0.8
	 */
	@Pure
	public boolean isGenerateCloneFunctions() {
		return getGeneratorConfig2().isGenerateCloneFunctions();
	}

	/** Set if the clone functions shall be generated.
	 *
	 * @param generateFunctions <code>true</code> if the functions shall be generated.
	 * @since 0.8
	 */
	public void setGenerateCloneFunctions(final boolean generateFunctions) {
		getGeneratorConfig2().setGenerateCloneFunctions(generateFunctions);
	}

	/** Replies if the serial number fields shall be generated.
	 *
	 * @return <code>true</code> if the fields shall be generated.
	 * @since 0.8
	 */
	@Pure
	public boolean isGenerateSerialNumberFields() {
		return getGeneratorConfig2().isGenerateSerialNumberFields();
	}

	/** Set if the serial number fields shall be generated.
	 *
	 * @param generateFields <code>true</code> if the fields shall be generated.
	 * @since 0.8
	 */
	public void setGenerateSerialNumberFields(final boolean generateFields) {
		getGeneratorConfig2().setGenerateSerialNumberFields(generateFields);
	}

	/** Change the source path.
	 *
	 * <p>The source path is a list the names of folders that are separated by {@link File#pathSeparator}.
	 *
	 * @param sourcePath the new source path.
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = new ArrayList<>();
		for (final String path : Strings.split(sourcePath, Pattern.quote(File.pathSeparator))) {
			this.sourcePath.add(normalizeFile(path));
		}
	}

	/** Change the source path.
	 *
	 * @param sourcePath the new source path.
	 */
	public void setSourcePath(Collection<File> sourcePath) {
		this.sourcePath = new ArrayList<>(sourcePath);
	}

	/** Add a folder to the source path.
	 *
	 * @param sourcePath the new source path.
	 */
	public void addSourcePath(String sourcePath) {
		if (!Strings.isEmpty(sourcePath)) {
			addSourcePath(normalizeFile(sourcePath));
		}
	}

	/** Add a folder to the source path.
	 *
	 * @param sourcePath the new source path.
	 */
	public void addSourcePath(File sourcePath) {
		if (this.sourcePath == null) {
			this.sourcePath = new ArrayList<>();
		}
		this.sourcePath.add(sourcePath);
	}

	/** Replies the source path.
	 *
	 * @return the source path.
	 */
	@Pure
	public List<File> getSourcePaths() {
		if (this.sourcePath == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(this.sourcePath);
	}

	private List<String> getSourcePathStrings() {
		if (this.sourcePath == null) {
			return Collections.emptyList();
		}
		final List<String> list = new ArrayList<>(this.sourcePath.size());
		for (final File input : this.sourcePath) {
			list.add(input.getAbsolutePath());
		}
		return list;
	}

	private void configureExtraLanguageGenerators() {
		final String generators = getExtraLanguageGenerators();
		if (Strings.isEmpty(generators)) {
			this.extraLanguageContributions.setContributionChecker(DISABLER);
		} else {
			final String[] identifiers = generators.split("\\s*" + Pattern.quote(File.pathSeparator) + "\\s*"); //$NON-NLS-1$ //$NON-NLS-2$
			this.extraLanguageContributions.setContributionChecker(it -> {
				for (final String id : identifiers) {
					if (it.isAcceptedIdentifier(id)) {
						return true;
					}
				}
				return false;
			});
		}
	}

	private void unconfigureExtraLanguageGenerators() {
		this.extraLanguageContributions.setContributionChecker(null);
	}

	/** Run the compilation.
	 *
	 * @return success status.
	 */
	@Inline(value = "compile((IProgressMonitor) null)", imported = {IProgressMonitor.class})
	public boolean compile() {
		return compile((IProgressMonitor) null);
	}

	/** Run the compilation.
	 *
	 * @param cancel is the tool for canceling the compilation.
	 * @return success status.
	 */
	public boolean compile(CancelIndicator cancel) {
		return compile(new IProgressMonitor() {
			@Override
			public void worked(int work) {
				//
			}

			@Override
			public void subTask(String name) {
				//
			}

			@Override
			public void setTaskName(String name) {
				//
			}

			@Override
			public void setCanceled(boolean value) {
				//
			}

			@Override
			public boolean isCanceled() {
				return cancel.isCanceled();
			}

			@Override
			public void internalWorked(double work) {
				//
			}

			@Override
			public void done() {
				//
			}

			@Override
			public void beginTask(String name, int totalWork) {
				//
			}
		});
	}

	/** Run the compilation.
	 *
	 * @param progress monitor of the progress of the compilation.
	 * @return success status.
	 * @since 0.8
	 */
	@SuppressWarnings({"checkstyle:npathcomplexity", "checkstyle:cyclomaticcomplexity",
		"checkstyle:returncount", "checkstyle:magicnumber"})
	public boolean compile(IProgressMonitor progress) {
		final IProgressMonitor monitor = progress == null ? new NullProgressMonitor() : progress;
		try {
			monitor.beginTask(Messages.SarlBatchCompiler_42, 18);
			if (!checkConfiguration(monitor)) {
				return false;
			}
			monitor.worked(1);
			final ResourceSet resourceSet = this.resourceSetProvider.get();
			configureExtraLanguageGenerators();
			if (!configureWorkspace(resourceSet, monitor)) {
				return false;
			}
			if (getLogger().isDebugEnabled()) {
				getLogger().debug(Utils.dump(getGeneratorConfig(), false));
			}
			monitor.worked(2);
			monitor.subTask(Messages.SarlBatchCompiler_43);
			if (this.generatorConfigProvider instanceof GeneratorConfigProvider) {
				((GeneratorConfigProvider) this.generatorConfigProvider).install(resourceSet, getGeneratorConfig());
			}
			if (monitor.isCanceled()) {
				return false;
			}
			if (this.generatorConfigProvider2 instanceof GeneratorConfigProvider2) {
				((GeneratorConfigProvider2) this.generatorConfigProvider2).install(resourceSet, getGeneratorConfig2());
			}
			if (getLogger().isDebugEnabled()) {
				getLogger().debug(Utils.dump(getGeneratorConfig2(), false));
			}
			if (monitor.isCanceled()) {
				return false;
			}
			monitor.worked(3);
			monitor.subTask(Messages.SarlBatchCompiler_44);
			final File stubClassDirectory = createTempDir(BINCLASS_FOLDER_PREFIX);
			if (monitor.isCanceled()) {
				return false;
			}
			monitor.worked(4);
			try {
				monitor.subTask(Messages.SarlBatchCompiler_45);
				this.compilerPhases.setIndexing(resourceSet, true);
				if (monitor.isCanceled()) {
					return false;
				}
				monitor.worked(5);
				// install a type provider without index lookup for the first phase
				installJvmTypeProvider(resourceSet, stubClassDirectory, true, monitor);
				if (monitor.isCanceled()) {
					return false;
				}
				monitor.worked(6);
				loadSARLFiles(resourceSet, monitor);
				if (monitor.isCanceled()) {
					return false;
				}
				monitor.worked(7);
				final File stubSourceDirectory = createStubs(resourceSet, monitor);
				if (monitor.isCanceled()) {
					return false;
				}
				monitor.worked(8);
				if (!preCompileStubs(stubSourceDirectory, stubClassDirectory, monitor)) {
					if (monitor.isCanceled()) {
						return false;
					}
					reportWarning(Messages.SarlBatchCompiler_2);
				}
				monitor.worked(9);
				if (!preCompileJava(stubSourceDirectory, stubClassDirectory, monitor)) {
					if (monitor.isCanceled()) {
						return false;
					}
					getLogger().debug(Messages.SarlBatchCompiler_3);
				}
				monitor.worked(10);
			} finally {
				monitor.subTask(Messages.SarlBatchCompiler_46);
				this.compilerPhases.setIndexing(resourceSet, false);
				if (monitor.isCanceled()) {
					return false;
				}
			}
			monitor.worked(11);
			// install a fresh type provider for the second phase, so we clear all previously cached classes and misses.
			installJvmTypeProvider(resourceSet, stubClassDirectory, false, monitor);
			if (monitor.isCanceled()) {
				return false;
			}
			monitor.worked(12);
			generateJvmElements(resourceSet, monitor);
			if (monitor.isCanceled()) {
				return false;
			}
			monitor.worked(13);
			final List<Resource> validatedResources = new ArrayList<>();
			final boolean hasError = validate(resourceSet, validatedResources, monitor);
			if (hasError || monitor.isCanceled()) {
				return false;
			}
			monitor.worked(14);
			overrideXtextInternalLoggers();
			generateJavaFiles(validatedResources, monitor);
			if (monitor.isCanceled()) {
				return false;
			}
			monitor.worked(15);
			if (isJavaPostCompilationEnable()) {
				postCompileJava(monitor);
				if (monitor.isCanceled()) {
					return false;
				}
			}
			monitor.worked(16);
		} finally {
			monitor.subTask(Messages.SarlBatchCompiler_47);
			destroyClassLoader(this.jvmTypesClassLoader);
			destroyClassLoader(this.annotationProcessingClassLoader);
			if (isDeleteTempDirectory()) {
				monitor.subTask(Messages.SarlBatchCompiler_48);
				for (final File file : this.tempFolders) {
					cleanFolder(file, ACCEPT_ALL_FILTER, true, true);
				}
			}
			unconfigureExtraLanguageGenerators();
			monitor.done();
		}
		return true;
	}

	/** Change the loggers that are internally used by Xtext.
	 */
	protected void overrideXtextInternalLoggers() {
		final Logger logger = getLogger();
		final org.apache.log4j.spi.LoggerFactory factory = new InternalXtextLoggerFactory(logger);
		final org.apache.log4j.Logger internalLogger = org.apache.log4j.Logger.getLogger(
				MessageFormat.format(Messages.SarlBatchCompiler_40, logger.getName()), factory);
		setStaticField(BatchLinkableResourceStorageWritable.class, "LOG", internalLogger); //$NON-NLS-1$
		setStaticField(BatchLinkableResource.class, "log", internalLogger); //$NON-NLS-1$
		setStaticField(ProcessorInstanceForJvmTypeProvider.class, "logger", internalLogger); //$NON-NLS-1$
	}

	private void setStaticField(Class<?> type, String name, org.apache.log4j.Logger logger) {
		try {
			final Field field = type.getDeclaredField(name);
			field.setAccessible(true);
			if ((field.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
				final Field modifiersField = Field.class.getDeclaredField("modifiers"); //$NON-NLS-1$
				modifiersField.setAccessible(true);
				modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			}
			field.set(null, logger);
		} catch (Exception exception) {
			reportError(exception.getLocalizedMessage(), exception);
		}
	}

	/** Create a message for the issue.
	 *
	 * @param issue the issue.
	 * @return the message.
	 */
	protected String createIssueMessage(Issue issue) {
		final IssueMessageFormatter formatter = getIssueMessageFormatter();
		final org.eclipse.emf.common.util.URI uriToProblem = issue.getUriToProblem();
		if (formatter != null) {
			final String message = formatter.format(issue, uriToProblem);
			if (message != null) {
				return message;
			}
		}
		if (uriToProblem != null) {
			final org.eclipse.emf.common.util.URI resourceUri = uriToProblem.trimFragment();
			return MessageFormat.format(Messages.SarlBatchCompiler_4,
					issue.getSeverity(), resourceUri.lastSegment(),
					resourceUri.isFile() ? resourceUri.toFileString() : "", //$NON-NLS-1$
							issue.getLineNumber(), issue.getColumn(), issue.getCode(), issue.getMessage());
		}
		return MessageFormat.format(Messages.SarlBatchCompiler_5,
				issue.getSeverity(), issue.getLineNumber(), issue.getColumn(), issue.getCode(), issue.getMessage());
	}

	/** Output the given issues.
	 *
	 * @param issues the issues to report.
	 */
	protected void reportIssues(Iterable<Issue> issues) {
		for (final Issue issue : issues) {
			final String issueMessage = createIssueMessage(issue);
			switch (issue.getSeverity()) {
			case ERROR:
				getLogger().error(issueMessage);
				break;
			case WARNING:
				getLogger().warn(issueMessage);
				break;
			case INFO:
				getLogger().info(issueMessage);
				break;
			case IGNORE:
			default:
				break;
			}
			notifiesIssueMessageListeners(issue, issue.getUriToProblem(), issueMessage);
		}
	}

	/** Reports the given warning message.
	 *
	 * @param message the warning message.
	 * @since 0.8
	 */
	protected void reportWarning(String message) {
		getLogger().warn(message);
		if (getReportInternalProblemsAsIssues()) {
			final org.eclipse.emf.common.util.URI uri  = null;
			final Issue.IssueImpl issue = new Issue.IssueImpl();
			issue.setCode(INTERNAL_ERROR_CODE);
			issue.setMessage(message);
			issue.setUriToProblem(uri);
			issue.setSeverity(Severity.WARNING);
			notifiesIssueMessageListeners(issue, uri, message);
		}
	}

	/** Reports the given warning message.
	 *
	 * @param message the warning message.
	 * @param exception the source of the exception.
	 * @since 0.8
	 */
	protected void reportWarning(String message, Throwable exception) {
		getLogger().warn(message, exception);
		if (getReportInternalProblemsAsIssues()) {
			final org.eclipse.emf.common.util.URI uri  = null;
			final Issue.IssueImpl issue = new Issue.IssueImpl();
			issue.setCode(INTERNAL_ERROR_CODE);
			issue.setMessage(message);
			issue.setUriToProblem(uri);
			issue.setSeverity(Severity.WARNING);
			notifiesIssueMessageListeners(issue, uri, message);
		}
	}

	/** Reports the given error message.
	 *
	 * @param message the warning message.
	 * @param exception the source of the exception.
	 * @since 0.8
	 */
	protected void reportError(String message, Throwable exception) {
		getLogger().error(message, exception);
		if (getReportInternalProblemsAsIssues()) {
			final org.eclipse.emf.common.util.URI uri  = null;
			final Issue.IssueImpl issue = new Issue.IssueImpl();
			issue.setCode(INTERNAL_ERROR_CODE);
			issue.setMessage(message);
			issue.setUriToProblem(uri);
			issue.setSeverity(Severity.ERROR);
			notifiesIssueMessageListeners(issue, uri, message);
		}
	}

	/** Reports the given error message.
	 *
	 * @param message the warning message.
	 * @param parameters the values of the parameters that must be dynamically replaced within the message text.
	 * @since 0.8
	 */
	protected void reportError(String message, Object... parameters) {
		getLogger().error(message, parameters);
		if (getReportInternalProblemsAsIssues()) {
			final org.eclipse.emf.common.util.URI uri  = null;
			final Issue.IssueImpl issue = new Issue.IssueImpl();
			issue.setCode(INTERNAL_ERROR_CODE);
			issue.setMessage(message);
			issue.setUriToProblem(uri);
			issue.setSeverity(Severity.ERROR);
			notifiesIssueMessageListeners(issue, uri, message);
		}
	}

	/** Generate the Java files from the SARL scripts.
	 *
	 * @param validatedResources the validatedResources for which the Java files could be generated.
	 * @param progress monitor of the progress of the compilation.
	 */
	protected void generateJavaFiles(Iterable<Resource> validatedResources, IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_49);
		getLogger().info(Messages.SarlBatchCompiler_28, getOutputPath());
		final JavaIoFileSystemAccess javaIoFileSystemAccess = this.javaIoFileSystemAccessProvider.get();
		javaIoFileSystemAccess.setOutputConfigurations(this.outputConfigurations);
		// The function configureWorkspace should set the output paths with absolute paths.
		//javaIoFileSystemAccess.setOutputPath(getOutputPath().getAbsolutePath());
		javaIoFileSystemAccess.setWriteTrace(isWriteTraceFiles());
		if (progress.isCanceled()) {
			return;
		}

		final GeneratorContext context = new GeneratorContext();
		context.setCancelIndicator(() -> progress.isCanceled());
		for (final Resource resource : validatedResources) {
			if (progress.isCanceled()) {
				return;
			}
			getLogger().debug(Messages.SarlBatchCompiler_23, resource.getURI().lastSegment());
			if (isWriteStorageFiles() && resource instanceof StorageAwareResource) {
				final StorageAwareResource storageAwareResource = (StorageAwareResource) resource;
				storageAwareResource.getResourceStorageFacade().saveResource(storageAwareResource, javaIoFileSystemAccess);
			}
			if (progress.isCanceled()) {
				return;
			}
			this.generator.generate(resource, javaIoFileSystemAccess, context);
			notifiesCompiledResourceReceiver(resource);
		}
	}

	/** Generate the JVM model elements.
	 *
	 * @param progress monitor of the progress of the compilation.
	 * @param resourceSet the container of the scripts.
	 */
	protected void generateJvmElements(ResourceSet resourceSet, IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_21);
		getLogger().info(Messages.SarlBatchCompiler_21);
		final List<Resource> resources = new LinkedList<>(resourceSet.getResources());
		for (final Resource resource : resources) {
			if (progress.isCanceled()) {
				return;
			}
			getLogger().debug(Messages.SarlBatchCompiler_26, resource.getURI().lastSegment());
			resource.getContents();
		}
		for (final Resource resource : resources) {
			if (progress.isCanceled()) {
				return;
			}
			getLogger().debug(Messages.SarlBatchCompiler_27, resource.getURI().lastSegment());
			EcoreUtil2.resolveLazyCrossReferences(resource, CancelIndicator.NullImpl);
		}
	}

	/** Generate the JVM model elements, and validate generated elements.
	 *
	 * @param resourceSet the container of the scripts.
	 * @param validResources will be filled by this function with the collection of resources that was successfully validated.
	 * @param progress monitor of the progress of the compilation.
	 * @return <code>true</code> if an error exists in the issues. Replies <code>false</code> if the activity is canceled.
	 */
	@SuppressWarnings({"checkstyle:cyclomaticcomplexity", "checkstyle:npathcomplexity"})
	protected boolean validate(ResourceSet resourceSet, Collection<Resource> validResources, IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_38);
		getLogger().info(Messages.SarlBatchCompiler_38);
		boolean hasError = false;
		final List<Resource> resources = new LinkedList<>(resourceSet.getResources());
		for (final Resource resource : resources) {
			if (progress.isCanceled()) {
				return false;
			}
			if (isSourceFile(resource)) {
				getLogger().debug(Messages.SarlBatchCompiler_22, resource.getURI().lastSegment());
				final IResourceServiceProvider resourceServiceProvider = IResourceServiceProvider.Registry.INSTANCE
						.getResourceServiceProvider(resource.getURI());
				if (resourceServiceProvider != null) {
					final IResourceValidator resourceValidator = resourceServiceProvider.getResourceValidator();
					final List<Issue> result = resourceValidator.validate(resource, CheckMode.ALL, null);
					if (progress.isCanceled()) {
						return false;
					}
					final SortedSet<Issue> issues = new TreeSet<>(getIssueComparator());
					boolean hasValidationError = false;
					for (final Issue issue : result) {
						if (progress.isCanceled()) {
							return false;
						}
						if (issue.isSyntaxError() || issue.getSeverity() == Severity.ERROR) {
							hasValidationError = true;
						}
						issues.add(issue);
					}
					hasError |= hasValidationError;
					if (!hasValidationError) {
						if (!issues.isEmpty()) {
							getLogger().debug(Messages.SarlBatchCompiler_39, resource.getURI().lastSegment());
							reportIssues(issues);
						}
						validResources.add(resource);
					} else {
						getLogger().debug(Messages.SarlBatchCompiler_39, resource.getURI().lastSegment());
						reportIssues(issues);
					}
				}
			}
		}
		return hasError;
	}

	/** Replies if the given resource is a script.
	 *
	 * @param resource the resource to test.
	 * @return <code>true</code> if the given resource is a script.
	 */
	@SuppressWarnings("static-method")
	protected boolean isSourceFile(Resource resource) {
		if (resource instanceof BatchLinkableResource) {
			return !((BatchLinkableResource) resource).isLoadedFromStorage();
		}
		return false;
	}

	/** Compile the stub files before the compilation of the project's files.
	 *
	 * @param sourceDirectory the source directory where stubs are stored.
	 * @param classDirectory the output directory, where stub binary files should be generated.
	 * @param progress monitor of the progress of the compilation.
	 * @return the success status. Replies <code>false</code> if the activity is canceled.
	 */
	protected boolean preCompileStubs(File sourceDirectory, File classDirectory, IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_50);
		return runJavaCompiler(classDirectory, Collections.singletonList(sourceDirectory), getClassPath(),
				false, false, progress);
	}

	/** Compile the java files before the compilation of the project's files.
	 *
	 * @param sourceDirectory the source directory where java files are stored.
	 * @param classDirectory the output directory, where binary files should be generated.
	 * @param progress monitor of the progress of the compilation.
	 * @return the success status. Replies <code>false</code> if the activity is canceled.
	 */
	protected boolean preCompileJava(File sourceDirectory, File classDirectory, IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_51);
		return runJavaCompiler(classDirectory, getSourcePaths(),
				Iterables.concat(Collections.singleton(sourceDirectory), getClassPath()),
				false, true, progress);
	}

	/** Compile the java files after the compilation of the project's files.
	 *
	 * @param progress monitor of the progress of the compilation.
	 * @return the success status. Replies <code>false</code> if the activity is canceled.
	 */
	protected boolean postCompileJava(IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_52);
		final File classOutputPath = getClassOutputPath();
		if (classOutputPath == null) {
			getLogger().info(Messages.SarlBatchCompiler_24);
			return true;
		}
		getLogger().info(Messages.SarlBatchCompiler_25);
		final Iterable<File> sources = Iterables.concat(getSourcePaths(), Collections.singleton(getOutputPath()));
		if (getLogger().isDebugEnabled()) {
			getLogger().debug(Messages.SarlBatchCompiler_29, toPathString(sources));
		}
		final List<File> classpath = getClassPath();
		if (getLogger().isDebugEnabled()) {
			getLogger().debug(Messages.SarlBatchCompiler_30, toPathString(classpath));
		}
		return runJavaCompiler(classOutputPath, sources, classpath, true, true, progress);
	}

	private static String toPathString(Iterable<File> files) {
		final StringBuilder result = new StringBuilder();
		for (final File file : files) {
			if (result.length() > 0) {
				result.append(File.pathSeparator);
			}
			result.append(file.toString());
		}
		return result.toString();
	}

	/** Run the Java compiler.
	 *
	 * @param classDirectory the output directory.
	 * @param sourcePathDirectories the source directories.
	 * @param classPathEntries classpath entries.
	 * @param enableCompilerOutput indicates if the Java compiler output is displayed.
	 * @param enableOptimization indicates if the Java compiler must applied optimization flags.
	 * @param progress monitor of the progress of the compilation.
	 * @return the success status. Replies <code>false</code> if the activity is canceled.
	 * @see IJavaBatchCompiler
	 */
	@SuppressWarnings({ "resource" })
	protected boolean runJavaCompiler(File classDirectory, Iterable<File> sourcePathDirectories,
			Iterable<File> classPathEntries, boolean enableCompilerOutput,
			boolean enableOptimization, IProgressMonitor progress) {
		String encoding = this.encodingProvider.getDefaultEncoding();
		if (Strings.isEmpty(encoding)) {
			encoding = null;
		}
		if (progress.isCanceled()) {
			return false;
		}
		final PrintWriter outWriter = getStubCompilerOutputWriter();
		final PrintWriter errWriter;
		if (enableCompilerOutput) {
			errWriter = getErrorCompilerOutputWriter();
		} else {
			errWriter = getStubCompilerOutputWriter();
		}
		if (progress.isCanceled()) {
			return false;
		}
		return getJavaCompiler().compile(
				classDirectory,
				sourcePathDirectories,
				classPathEntries,
				getBootClassPath(),
				getJavaSourceVersion(),
				encoding,
				isJavaCompilerVerbose(),
				enableOptimization ? getOptimizationLevel() : null,
				outWriter,
				errWriter,
				getLogger(),
				progress);
	}

	private PrintWriter getStubCompilerOutputWriter() {
		final Writer debugWriter = new Writer() {
			@Override
			public void write(char[] data, int offset, int count) throws IOException {
				final String message = String.copyValueOf(data, offset, count);
				getLogger().debug(message);
			}

			@Override
			public void flush() throws IOException {
				//
			}

			@Override
			public void close() throws IOException {
				//
			}
		};
		return new PrintWriter(debugWriter);
	}

	private PrintWriter getErrorCompilerOutputWriter() {
		final Writer debugWriter = new Writer() {
			@Override
			public void write(char[] data, int offset, int count) throws IOException {
				final String message = String.copyValueOf(data, offset, count);
				reportError(message);
			}

			@Override
			public void flush() throws IOException {
				//
			}

			@Override
			public void close() throws IOException {
				//
			}
		};
		return new PrintWriter(debugWriter);
	}

	/** Create the stubs.
	 *
	 * @param resourceSet the input resource set.
	 * @param progress monitor of the progress of the compilation.
	 * @return the folder in which the stubs are located. Replies <code>null</code> if the activity is canceled.
	 */
	protected File createStubs(ResourceSet resourceSet, IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_53);
		final File outputDirectory = createTempDir(STUB_FOLDER_PREFIX);
		if (progress.isCanceled()) {
			return null;
		}
		getLogger().debug(Messages.SarlBatchCompiler_19, outputDirectory);
		final JavaIoFileSystemAccess fileSystemAccess = this.javaIoFileSystemAccessProvider.get();
		if (progress.isCanceled()) {
			return null;
		}
		fileSystemAccess.setOutputPath(outputDirectory.toString());
		final List<Resource> resources = new ArrayList<>(resourceSet.getResources());
		for (final Resource resource : resources) {
			if (progress.isCanceled()) {
				return null;
			}
			getLogger().debug(Messages.SarlBatchCompiler_20, resource.getURI());
			final IResourceDescription description = this.resourceDescriptionManager.getResourceDescription(resource);
			this.stubGenerator.doGenerateStubs(fileSystemAccess, description);
		}
		return outputDirectory;
	}

	/** Load the SARL files in the given resource set.
	 *
	 * @param progress monitor of the progress of the compilation.
	 * @param resourceSet the resource set to load from.
	 */
	protected void loadSARLFiles(ResourceSet resourceSet, IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_54);
		this.encodingProvider.setDefaultEncoding(getFileEncoding());
		final NameBasedFilter nameBasedFilter = new NameBasedFilter();
		nameBasedFilter.setExtension(this.fileExtensionProvider.getPrimaryFileExtension());
		final PathTraverser pathTraverser = new PathTraverser();
		final List<String> sourcePathDirectories = getSourcePathStrings();
		if (progress.isCanceled()) {
			return;
		}
		final Multimap<String, org.eclipse.emf.common.util.URI> pathes = pathTraverser.resolvePathes(sourcePathDirectories,
				input -> nameBasedFilter.matches(input));
		if (progress.isCanceled()) {
			return;
		}
		for (final String source : pathes.keySet()) {
			for (final org.eclipse.emf.common.util.URI uri : pathes.get(source)) {
				if (progress.isCanceled()) {
					return;
				}
				getLogger().debug(Messages.SarlBatchCompiler_7, uri);
				resourceSet.getResource(uri, true);
			}
		}
	}

	/** Create a temporary subdirectory inside the root temp directory.
	 *
	 * @param namePrefix the prefix for the folder name.
	 * @return the temp directory.
	 * @see #getTempDirectory()
	 */
	protected File createTempDir(String namePrefix) {
		final File tempDir = new File(getTempDirectory(), namePrefix);
		cleanFolder(tempDir, ACCEPT_ALL_FILTER, true, true);
		if (!tempDir.mkdirs()) {
			throw new RuntimeException(MessageFormat.format(Messages.SarlBatchCompiler_8, tempDir.getAbsolutePath()));
		}
		this.tempFolders.add(tempDir);
		return tempDir;
	}

	/** Clean the folders.
	 *
	 * @param parentFolder the parent folder.
	 * @param filter the file filter for the file to remove..
	 * @param continueOnError indicates if the cleaning should continue on error.
	 * @param deleteParentFolder indicates if the parent folder should be removed.
	 * @return the success status.
	 */
	protected boolean cleanFolder(File parentFolder, FileFilter filter, boolean continueOnError,
			boolean deleteParentFolder) {
		try {
			getLogger().debug(Messages.SarlBatchCompiler_9, parentFolder.toString());
			return Files.cleanFolder(parentFolder, null, continueOnError, deleteParentFolder);
		} catch (FileNotFoundException e) {
			return true;
		}
	}

	/** Check the compiler configuration; and logs errors.
	 *
	 * @param progress monitor of the progress of the compilation.
	 * @return success status. Replies <code>false</code> if the operation is canceled.
	 */
	protected boolean checkConfiguration(IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_55);
		final File output = getOutputPath();
		getLogger().debug(Messages.SarlBatchCompiler_35, output);
		if (output == null) {
			reportError(Messages.SarlBatchCompiler_36);
			return false;
		}
		progress.subTask(Messages.SarlBatchCompiler_56);
		for (final File sourcePath : getSourcePaths()) {
			if (progress.isCanceled()) {
				return false;
			}
			try {
				getLogger().debug(Messages.SarlBatchCompiler_37, sourcePath);
				if (isContainedIn(output.getCanonicalFile(), sourcePath.getCanonicalFile())) {
					reportError(Messages.SarlBatchCompiler_10, output, sourcePath);
					return false;
				}
			} catch (IOException e) {
				reportError(Messages.SarlBatchCompiler_11, e);
			}
		}
		return true;
	}

	private static boolean isContainedIn(File child, File possibleParent) {
		File parent = child;
		while (parent != null) {
			if (parent.equals(possibleParent)) {
				return true;
			}
			parent = parent.getParentFile();
		}
		return false;
	}

	private static LinkedList<String> splitFile(File file, IProgressMonitor progress) {
		assert progress != null;
		final LinkedList<String> elements = new LinkedList<>();
		File current = file;
		do {
			if (progress.isCanceled()) {
				return null;
			}
			elements.addFirst(current.getName());
			current = current.getParentFile();
		} while (current != null);
		return elements;
	}

	@SuppressWarnings({"checkstyle:cyclomaticcomplexity", "checkstyle:npathcomplexity"})
	private File determineCommonRoot(Iterable<File> files, IProgressMonitor progress) {
		assert progress != null;

		if (this.baseUri != null) {
			if (this.baseUri.isFile()) {
				getLogger().debug(Messages.SarlBatchCompiler_32, this.baseUri);
				return new File(this.baseUri.toFileString());
			}
			getLogger().debug(Messages.SarlBatchCompiler_33, this.baseUri);
		}

		LinkedList<String> longuestPrefix = null;

		for (final File file : files) {
			if (progress.isCanceled()) {
				return null;
			}
			if (file != null) {
				final LinkedList<String> components = splitFile(file, progress);
				if (longuestPrefix == null) {
					longuestPrefix = components;
				} else {
					int i = 0;
					while (i < longuestPrefix.size() && i < components.size()
							&& Strings.equal(longuestPrefix.get(i), components.get(i))) {
						if (progress.isCanceled()) {
							return null;
						}
						++i;
					}
					while (i < longuestPrefix.size()) {
						if (progress.isCanceled()) {
							return null;
						}
						longuestPrefix.removeLast();
					}
					if (longuestPrefix.isEmpty()) {
						return null;
					}
				}
			}
		}

		if (longuestPrefix == null || progress.isCanceled()) {
			return null;
		}

		File prefix = null;
		for (final String component : longuestPrefix) {
			if (progress.isCanceled()) {
				return null;
			}
			if (prefix == null) {
				prefix = new File(component);
			} else {
				prefix = new File(prefix, component);
			}
		}

		return prefix;
	}

	@SuppressWarnings({"checkstyle:cyclomaticcomplexity", "checkstyle:npathcomplexity", "checkstyle:returncount"})
	private boolean configureWorkspace(ResourceSet resourceSet, IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_57);
		final List<File> sourceFolders = getSourcePaths();
		final File javaOutputFile = getOutputPath();
		final File classOutputFile = getClassOutputPath();
		if (sourceFolders == null || sourceFolders.isEmpty() || javaOutputFile == null
				|| classOutputFile == null || progress.isCanceled()) {
			if (sourceFolders == null || sourceFolders.isEmpty()) {
				reportError(Messages.SarlBatchCompiler_60);
			}
			if (javaOutputFile == null) {
				reportError(Messages.SarlBatchCompiler_61);
			}
			if (classOutputFile == null) {
				reportError(Messages.SarlBatchCompiler_62);
			}
			return false;
		}

		getLogger().debug(Messages.SarlBatchCompiler_31, this.baseUri);

		final File commonRoot = determineCommonRoot(
				Iterables.concat(sourceFolders, Arrays.asList(javaOutputFile, classOutputFile)),
				progress);
		if (progress.isCanceled()) {
			return false;
		}

		getLogger().debug(Messages.SarlBatchCompiler_34, commonRoot);
		if (commonRoot == null) {
			reportError(Messages.SarlBatchCompiler_12);
			for (final File sourceFile : sourceFolders) {
				reportError(Messages.SarlBatchCompiler_13, sourceFile);
			}
			reportError(Messages.SarlBatchCompiler_14, javaOutputFile);
			return false;
		}
		this.projectConfig = new FileProjectConfig(commonRoot, commonRoot.getName());
		if (progress.isCanceled()) {
			return false;
		}

		final URI commonURI = commonRoot.toURI();
		final URI relativizedTarget = commonURI.relativize(javaOutputFile.toURI());
		if (progress.isCanceled()) {
			return false;
		}
		if (relativizedTarget.isAbsolute()) {
			reportError(Messages.SarlBatchCompiler_15, javaOutputFile, commonRoot);
			return false;
		}
		final CharMatcher slash = CharMatcher.is('/');
		final String relativeTargetFolder = slash.trimTrailingFrom(relativizedTarget.getPath());
		final Set<OutputConfiguration> allOutputConfigurations = this.outputConfigurationProvider.getOutputConfigurations();
		if (progress.isCanceled()) {
			return false;
		}
		this.outputConfigurations = new TreeMap<>();
		for (final OutputConfiguration configuration : allOutputConfigurations) {
			if (progress.isCanceled()) {
				return false;
			}
			this.outputConfigurations.put(configuration.getName(), configuration);
			if (Strings.equal(configuration.getName(), IFileSystemAccess.DEFAULT_OUTPUT)) {
				configuration.setOutputDirectory(new File(commonRoot, relativeTargetFolder).getAbsolutePath());
			} else {
				File outFile = new File(configuration.getOutputDirectory());
				outFile = new File(commonRoot, outFile.getPath());
				configuration.setOutputDirectory(outFile.getAbsolutePath());
			}
		}
		if (progress.isCanceled()) {
			return false;
		}
		for (final File source : sourceFolders) {
			if (progress.isCanceled()) {
				return false;
			}
			final URI relSource = commonURI.relativize(source.toURI());
			if (relSource.isAbsolute()) {
				reportError(Messages.SarlBatchCompiler_16, source, commonRoot);
				return false;
			}
			this.projectConfig.addSourceFolder(slash.trimTrailingFrom(relSource.getPath()));
		}
		if (progress.isCanceled()) {
			return false;
		}
		final Map<String, Set<OutputConfiguration>> outputConfigurations = new HashMap<>();
		outputConfigurations.put(this.languageName, allOutputConfigurations);
		ProjectConfigAdapter.install(resourceSet, this.projectConfig);
		resourceSet.eAdapters().add(new OutputConfigurationAdapter(outputConfigurations));
		if (progress.isCanceled()) {
			return false;
		}
		return true;
	}

	/**
	 * Installs the JvmTypeProvider optionally including index access into the {@link ResourceSet}. The lookup classpath
	 * is enhanced with the given tmp directory.
	 *
	 * @param resourceSet the resource set that will be compiled.
	 * @param temporaryClassDirectory the directory where the class files of the stubs are generated.
	 * @param skipIndexLookup indicates if the index should be used for looking up types.
	 * @param cancelIndicator monitor for cancelling the compilation.
	 */
	@SuppressWarnings({ "resource", "unused", "checkstyle:npathcomplexity" })
	private void installJvmTypeProvider(ResourceSet resourceSet, File temporaryClassDirectory, boolean skipIndexLookup,
			IProgressMonitor progress) {
		assert progress != null;
		progress.subTask(Messages.SarlBatchCompiler_58);
		final Iterable<File> classpath;
		if (temporaryClassDirectory != null) {
			classpath = Iterables.concat(getClassPath(), getSourcePaths(),
					Collections.singletonList(temporaryClassDirectory));
		} else {
			classpath = Iterables.concat(getClassPath(), getSourcePaths());
		}
		getLogger().debug(Messages.SarlBatchCompiler_17, classpath);
		if (progress.isCanceled()) {
			return;
		}
		final ClassLoader parentClassLoader;
		if (isUseCurrentClassLoaderAsParent()) {
			parentClassLoader = getClass().getClassLoader();
		} else if (getBootClassPath().isEmpty()) {
			parentClassLoader = getCurrentClassLoader();
		} else {
			parentClassLoader = new AlternateJdkLoader(getBootClassPath());
		}
		if (progress.isCanceled()) {
			return;
		}
		this.jvmTypesClassLoader = createClassLoader(classpath, parentClassLoader);
		if (progress.isCanceled()) {
			return;
		}
		new ClasspathTypeProvider(this.jvmTypesClassLoader, resourceSet, skipIndexLookup ? null : this.indexedJvmTypeAccess, null);
		if (progress.isCanceled()) {
			return;
		}
		((XtextResourceSet) resourceSet).setClasspathURIContext(this.jvmTypesClassLoader);
		if (progress.isCanceled()) {
			return;
		}

		// for annotation processing we need to have the compiler's classpath as a parent.
		progress.subTask(Messages.SarlBatchCompiler_59);
		this.annotationProcessingClassLoader = createClassLoader(classpath, getCurrentClassLoader());
		if (progress.isCanceled()) {
			return;
		}
		resourceSet.eAdapters().add(new ProcessorInstanceForJvmTypeProvider.ProcessorClassloaderAdapter(this.annotationProcessingClassLoader));
	}

	/** Create the project class loader.
	 *
	 * @param jarsAndFolders the project class path.
	 * @param parentClassLoader the parent class loader.
	 * @return the class loader for the project.
	 */
	@SuppressWarnings("static-method")
	protected ClassLoader createClassLoader(Iterable<File> jarsAndFolders, ClassLoader parentClassLoader) {
		return new URLClassLoader(Iterables.toArray(Iterables.transform(jarsAndFolders, from -> {
			try {
				final URL url = from.toURI().toURL();
				assert url != null;
				return url;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}), URL.class), parentClassLoader);
	}

	/** Null-safe destruction of the given class loaders.
	 *
	 * @param classLoader the class loader to destroy.
	 */
	protected void destroyClassLoader(ClassLoader classLoader) {
		if (classLoader instanceof Closeable) {
			try {
				((Closeable) classLoader).close();
			} catch (Exception e) {
				reportWarning(Messages.SarlBatchCompiler_18, e);
			}
		}
	}

	/** Change the severity level of a warning.
	 *
	 * @param warningId the identifier of the warning. If {@code null} or empty, this function does nothing.
	 * @param severity the new severity. If {@code null} this function does nothing.
	 * @since 0.5
	 */
	public void setWarningSeverity(String warningId, Severity severity) {
		if (!Strings.isEmpty(warningId) && severity != null) {
			this.issueSeverityProvider.setSeverity(warningId, severity);
		}
	}

	/** Change the severity level of for all the warnings.
	 *
	 * @param severity the new severity. If {@code null} this function does nothing.
	 * @since 0.5
	 */
	public void setAllWarningSeverities(Severity severity) {
		if (severity != null) {
			this.issueSeverityProvider.setAllSeverities(severity);
		}
	}

	/** Formatter for the issue messages.
	 *
	 * @author $Author: sgalland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	@FunctionalInterface
	public interface IssueMessageFormatter {

		/** Format the message for the given issue.
		 *
		 * @param issue the issue.
		 * @param uri URI to the problem.
		 * @return the message; or {@code null} for using the default formatter.
		 */
		String format(Issue issue, org.eclipse.emf.common.util.URI uri);

	}

	/** Listener for the issue messages.
	 *
	 * @author $Author: sgalland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	@FunctionalInterface
	public interface IssueMessageListener {

		/** Replies the message for the given issue.
		 *
		 * @param issue the issue.
		 * @param uri URI to the problem.
		 * @param message the formatted message.
		 */
		void onIssue(Issue issue, org.eclipse.emf.common.util.URI uri, String message);

	}

	/** Comparator of issues.
	 *
	 * @author $Author: sgalland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	public static class DefaultIssueComparator implements Comparator<Issue> {

		private static int compareSafe(Integer n1, Integer n2) {
			if (n1 == null) {
				return n2 == null ? 0 : -1;
			}
			if (n2 == null) {
				return 1;
			}
			return Integer.compare(n1.intValue(), n2.intValue());
		}

		private static int compareSafe(Severity s1, Severity s2) {
			if (s1 == null) {
				return s2 == null ? 0 : -1;
			}
			if (s2 == null) {
				return 1;
			}
			return s1.compareTo(s2);
		}

		private static int compareSafe(String s1, String s2) {
			if (s1 == null) {
				return s2 == null ? 0 : -1;
			}
			if (s2 == null) {
				return 1;
			}
			return s1.compareTo(s2);
		}

		@Override
		@SuppressWarnings("checkstyle:npathcomplexity")
		public int compare(Issue issue1, Issue issue2) {
			if (issue1 == issue2) {
				return 0;
			}
			if (issue1 == null) {
				return -1;
			}
			if (issue2 == null) {
				return 1;
			}
			final org.eclipse.emf.common.util.URI u1 = issue1.getUriToProblem();
			final org.eclipse.emf.common.util.URI u2 = issue2.getUriToProblem();
			int cmp = 0;
			if (u1 != u2 && u1 != null && u2 != null) {
				cmp = u1.toFileString().compareTo(u2.toFileString());
			}
			if (cmp != 0) {
				return cmp;
			}
			cmp = compareSafe(issue1.getLineNumber(), issue2.getLineNumber());
			if (cmp != 0) {
				return cmp;
			}
			cmp = compareSafe(issue1.getColumn(), issue2.getColumn());
			if (cmp != 0) {
				return cmp;
			}
			cmp = compareSafe(issue1.getSeverity(), issue2.getSeverity());
			if (cmp != 0) {
				return cmp;
			}
			cmp = compareSafe(issue1.getMessage(), issue2.getMessage());
			if (cmp != 0) {
				return cmp;
			}
			return Integer.compare(System.identityHashCode(issue1), System.identityHashCode(issue2));
		}

	}

}
