/*
 * Copyright (C) 2014-2018 the original authors or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sarl.lang.core.tests.scoping.extensions.time;

import com.google.inject.Inject;
import org.eclipse.xtext.xbase.testing.CompilationTestHelper;
import org.junit.Test;

import io.sarl.lang.SARLVersion;
import io.sarl.lang.sarl.SarlPackage;
import io.sarl.tests.api.AbstractSarlTest;

/**
 * @author $Author: srodriguez$
 * @author $Author: sgalland$
 * @version $Name$ $Revision$ $Date$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
@SuppressWarnings("all")
public class TimeExtensionsCompilerTest extends AbstractSarlTest {

	@Test
	public void milliseconds() throws Exception {
		String source = multilineString(
				"package io.sarl.lang.tests.ste",
				"agent A1 {",
					"def myaction0 : Object {",
					"	1234.milliseconds",
					"}",
				"}"
			);
		String expected = multilineString(
				"package io.sarl.lang.tests.ste;",
				"",
				"import io.sarl.lang.annotation.SarlElementType;",
				"import io.sarl.lang.annotation.SarlSpecification;",
				"import io.sarl.lang.annotation.SyntheticMember;",
				"import io.sarl.lang.core.Agent;",
				"import io.sarl.lang.core.BuiltinCapacitiesProvider;",
				"import io.sarl.lang.core.DynamicSkillProvider;",
				"import java.util.UUID;",
				"import javax.inject.Inject;",
				"import org.eclipse.xtext.xbase.lib.Pure;",
				"",
				"@SarlSpecification(\"" + SARLVersion.SPECIFICATION_RELEASE_VERSION_STRING + "\")",
				"@SarlElementType(" + SarlPackage.SARL_AGENT + ")",
				"@SuppressWarnings(\"all\")",
				"public class A1 extends Agent {",
				"  @Pure",
				"  protected Object myaction0() {",
				"    return Long.valueOf((1234));",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  public A1(final UUID arg0, final UUID arg1) {",
				"    super(arg0, arg1);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  @Deprecated",
				"  @Inject",
				"  public A1(final BuiltinCapacitiesProvider arg0, final UUID arg1, final UUID arg2) {",
				"    super(arg0, arg1, arg2);",
				"  }",
				"  ", 
				"  @SyntheticMember", 
				"  @Inject", 
				"  public A1(final UUID arg0, final UUID arg1, final DynamicSkillProvider arg2) {", 
				"    super(arg0, arg1, arg2);", 
				"  }",
				"}",
				""
				);
		getCompileHelper().assertCompilesTo(source, expected);
	}

	@Test
	public void seconds() throws Exception {
		String source = multilineString(
				"package io.sarl.lang.tests.ste",
				"agent A1 {",
					"def myaction0 : Object {",
					"	1234.seconds",
					"}",
				"}"
			);
		String expected = multilineString(
				"package io.sarl.lang.tests.ste;",
				"",
				"import io.sarl.lang.annotation.SarlElementType;",
				"import io.sarl.lang.annotation.SarlSpecification;",
				"import io.sarl.lang.annotation.SyntheticMember;",
				"import io.sarl.lang.core.Agent;",
				"import io.sarl.lang.core.BuiltinCapacitiesProvider;",
				"import io.sarl.lang.core.DynamicSkillProvider;",
				"import java.util.UUID;",
				"import javax.inject.Inject;",
				"import org.eclipse.xtext.xbase.lib.Pure;",
				"",
				"@SarlSpecification(\"" + SARLVersion.SPECIFICATION_RELEASE_VERSION_STRING + "\")",
				"@SarlElementType(" + SarlPackage.SARL_AGENT + ")",
				"@SuppressWarnings(\"all\")",
				"public class A1 extends Agent {",
				"  @Pure",
				"  protected Object myaction0() {",
				"    return Long.valueOf((1234) * 1000);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  public A1(final UUID arg0, final UUID arg1) {",
				"    super(arg0, arg1);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  @Deprecated",
				"  @Inject",
				"  public A1(final BuiltinCapacitiesProvider arg0, final UUID arg1, final UUID arg2) {",
				"    super(arg0, arg1, arg2);",
				"  }",
				"  ", 
				"  @SyntheticMember", 
				"  @Inject", 
				"  public A1(final UUID arg0, final UUID arg1, final DynamicSkillProvider arg2) {", 
				"    super(arg0, arg1, arg2);", 
				"  }",
				"}",
				""
				);
		getCompileHelper().assertCompilesTo(source, expected);
	}

	@Test
	public void minutes() throws Exception {
		String source = multilineString(
				"package io.sarl.lang.tests.ste",
				"agent A1 {",
					"def myaction0 : Object {",
					"	1234.minutes",
					"}",
				"}"
			);
		String expected = multilineString(
				"package io.sarl.lang.tests.ste;",
				"",
				"import io.sarl.lang.annotation.SarlElementType;",
				"import io.sarl.lang.annotation.SarlSpecification;",
				"import io.sarl.lang.annotation.SyntheticMember;",
				"import io.sarl.lang.core.Agent;",
				"import io.sarl.lang.core.BuiltinCapacitiesProvider;",
				"import io.sarl.lang.core.DynamicSkillProvider;",
				"import java.util.UUID;",
				"import javax.inject.Inject;",
				"import org.eclipse.xtext.xbase.lib.Pure;",
				"",
				"@SarlSpecification(\"" + SARLVersion.SPECIFICATION_RELEASE_VERSION_STRING + "\")",
				"@SarlElementType(" + SarlPackage.SARL_AGENT + ")",
				"@SuppressWarnings(\"all\")",
				"public class A1 extends Agent {",
				"  @Pure",
				"  protected Object myaction0() {",
				"    return Long.valueOf((1234) * 60000);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  public A1(final UUID arg0, final UUID arg1) {",
				"    super(arg0, arg1);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  @Deprecated",
				"  @Inject",
				"  public A1(final BuiltinCapacitiesProvider arg0, final UUID arg1, final UUID arg2) {",
				"    super(arg0, arg1, arg2);",
				"  }",
				"  ", 
				"  @SyntheticMember", 
				"  @Inject", 
				"  public A1(final UUID arg0, final UUID arg1, final DynamicSkillProvider arg2) {", 
				"    super(arg0, arg1, arg2);", 
				"  }",
				"}",
				""
				);
		getCompileHelper().assertCompilesTo(source, expected);
	}

	@Test
	public void hours() throws Exception {
		String source = multilineString(
				"package io.sarl.lang.tests.ste",
				"agent A1 {",
					"def myaction0 : Object {",
					"	1234.hours",
					"}",
				"}"
			);
		String expected = multilineString(
				"package io.sarl.lang.tests.ste;",
				"",
				"import io.sarl.lang.annotation.SarlElementType;",
				"import io.sarl.lang.annotation.SarlSpecification;",
				"import io.sarl.lang.annotation.SyntheticMember;",
				"import io.sarl.lang.core.Agent;",
				"import io.sarl.lang.core.BuiltinCapacitiesProvider;",
				"import io.sarl.lang.core.DynamicSkillProvider;",
				"import java.util.UUID;",
				"import javax.inject.Inject;",
				"import org.eclipse.xtext.xbase.lib.Pure;",
				"",
				"@SarlSpecification(\"" + SARLVersion.SPECIFICATION_RELEASE_VERSION_STRING + "\")",
				"@SarlElementType(" + SarlPackage.SARL_AGENT + ")",
				"@SuppressWarnings(\"all\")",
				"public class A1 extends Agent {",
				"  @Pure",
				"  protected Object myaction0() {",
				"    return Long.valueOf((1234) * 3600000);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  public A1(final UUID arg0, final UUID arg1) {",
				"    super(arg0, arg1);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  @Deprecated",
				"  @Inject",
				"  public A1(final BuiltinCapacitiesProvider arg0, final UUID arg1, final UUID arg2) {",
				"    super(arg0, arg1, arg2);",
				"  }",
				"  ", 
				"  @SyntheticMember", 
				"  @Inject", 
				"  public A1(final UUID arg0, final UUID arg1, final DynamicSkillProvider arg2) {", 
				"    super(arg0, arg1, arg2);", 
				"  }",
				"}",
				""
				);
		getCompileHelper().assertCompilesTo(source, expected);
	}

	@Test
	public void days() throws Exception {
		String source = multilineString(
				"package io.sarl.lang.tests.ste",
				"agent A1 {",
					"def myaction0 : Object {",
					"	1234.days",
					"}",
				"}"
			);
		String expected = multilineString(
				"package io.sarl.lang.tests.ste;",
				"",
				"import io.sarl.lang.annotation.SarlElementType;",
				"import io.sarl.lang.annotation.SarlSpecification;",
				"import io.sarl.lang.annotation.SyntheticMember;",
				"import io.sarl.lang.core.Agent;",
				"import io.sarl.lang.core.BuiltinCapacitiesProvider;",
				"import io.sarl.lang.core.DynamicSkillProvider;",
				"import java.util.UUID;",
				"import javax.inject.Inject;",
				"import org.eclipse.xtext.xbase.lib.Pure;",
				"",
				"@SarlSpecification(\"" + SARLVersion.SPECIFICATION_RELEASE_VERSION_STRING + "\")",
				"@SarlElementType(" + SarlPackage.SARL_AGENT + ")",
				"@SuppressWarnings(\"all\")",
				"public class A1 extends Agent {",
				"  @Pure",
				"  protected Object myaction0() {",
				"    return Long.valueOf((1234) * 86400000);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  public A1(final UUID arg0, final UUID arg1) {",
				"    super(arg0, arg1);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  @Deprecated",
				"  @Inject",
				"  public A1(final BuiltinCapacitiesProvider arg0, final UUID arg1, final UUID arg2) {",
				"    super(arg0, arg1, arg2);",
				"  }",
				"  ", 
				"  @SyntheticMember", 
				"  @Inject", 
				"  public A1(final UUID arg0, final UUID arg1, final DynamicSkillProvider arg2) {", 
				"    super(arg0, arg1, arg2);", 
				"  }",
				"}",
				""
				);
		getCompileHelper().assertCompilesTo(source, expected);
	}

	@Test
	public void weeks() throws Exception {
		String source = multilineString(
				"package io.sarl.lang.tests.ste",
				"agent A1 {",
					"def myaction0 : Object {",
					"	1234.weeks",
					"}",
				"}"
			);
		String expected = multilineString(
				"package io.sarl.lang.tests.ste;",
				"",
				"import io.sarl.lang.annotation.SarlElementType;",
				"import io.sarl.lang.annotation.SarlSpecification;",
				"import io.sarl.lang.annotation.SyntheticMember;",
				"import io.sarl.lang.core.Agent;",
				"import io.sarl.lang.core.BuiltinCapacitiesProvider;",
				"import io.sarl.lang.core.DynamicSkillProvider;",
				"import java.util.UUID;",
				"import javax.inject.Inject;",
				"import org.eclipse.xtext.xbase.lib.Pure;",
				"",
				"@SarlSpecification(\"" + SARLVersion.SPECIFICATION_RELEASE_VERSION_STRING + "\")",
				"@SarlElementType(" + SarlPackage.SARL_AGENT + ")",
				"@SuppressWarnings(\"all\")",
				"public class A1 extends Agent {",
				"  @Pure",
				"  protected Object myaction0() {",
				"    return Long.valueOf((1234) * 604800000);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  public A1(final UUID arg0, final UUID arg1) {",
				"    super(arg0, arg1);",
				"  }",
				"  ",
				"  @SyntheticMember",
				"  @Deprecated",
				"  @Inject",
				"  public A1(final BuiltinCapacitiesProvider arg0, final UUID arg1, final UUID arg2) {",
				"    super(arg0, arg1, arg2);",
				"  }",
				"  ", 
				"  @SyntheticMember", 
				"  @Inject", 
				"  public A1(final UUID arg0, final UUID arg1, final DynamicSkillProvider arg2) {", 
				"    super(arg0, arg1, arg2);", 
				"  }",
				"}",
				""
				);
		getCompileHelper().assertCompilesTo(source, expected);
	}

}
