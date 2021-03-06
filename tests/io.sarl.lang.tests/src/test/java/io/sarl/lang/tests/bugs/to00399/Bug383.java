/*
 * $Id$
 * 
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
package io.sarl.lang.tests.bugs.to00399;

import com.google.inject.Inject;
import org.eclipse.xtext.xbase.testing.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.sarl.lang.SARLVersion;
import io.sarl.lang.sarl.SarlPackage;
import io.sarl.lang.sarl.SarlScript;
import io.sarl.tests.api.AbstractSarlTest;

/**
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
@RunWith(Suite.class)
@SuiteClasses({
	Bug383.ParserTest.class,
	Bug383.CompilerTest.class,
})
@SuppressWarnings("all")
public class Bug383 {

	protected static String snippet = AbstractSarlTest.multilineString(
			"agent A1 {",
			"  /** Testing documentation generator.",
			"   * @param type the type.",
			"   * @param defaultValue the default value.",
			"   * @return the value.",
			"   */",
			"  def getInstance(type : String, defaultValue : String = null) : String {",
			"    return null",
			"  }",
			"}");

	public static class ParserTest extends AbstractSarlTest {

		@Test
		public void parsing() throws Exception {
			SarlScript mas = file(snippet);
			validate(mas).assertNoErrors();
		}

	}

	public static class CompilerTest extends AbstractSarlTest {
		
		@Test
		public void compilation() throws Exception {
			final String expected = multilineString(
					"import io.sarl.lang.annotation.DefaultValue;",
					"import io.sarl.lang.annotation.DefaultValueSource;",
					"import io.sarl.lang.annotation.DefaultValueUse;",
					"import io.sarl.lang.annotation.SarlElementType;",
					"import io.sarl.lang.annotation.SarlSourceCode;",
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
					"  /**",
					"   * Testing documentation generator.",
					"   * @param type the type.",
					"   * @param defaultValue the default value.",
					"   * @return the value.",
					"   */",
					"  @DefaultValueSource",
					"  @Pure",
					"  protected String getInstance(final String type, @DefaultValue(\"A1#GETINSTANCE_0\") final String defaultValue) {",
					"    return null;",
					"  }",
					"  ",
					"  /**",
					"   * Default value for the parameter defaultValue",
					"   */",
					"  @SyntheticMember",
					"  @SarlSourceCode(\"null\")",
					"  private final static String $DEFAULT_VALUE$GETINSTANCE_0 = null;",
					"  ",
					"  /**",
					"   * Testing documentation generator.",
					"   * @param type the type.",
					"   * @optionalparam defaultValue the default value.",
					"   * @return the value.",
					"   */",
					"  @DefaultValueUse(\"java.lang.String,java.lang.String\")",
					"  @SyntheticMember",
					"  @Pure",
					"  protected final String getInstance(final String type) {",
					"    return getInstance(type, $DEFAULT_VALUE$GETINSTANCE_0);",
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
					"");

			getCompileHelper().compile(snippet, 
					(r) -> assertEquals(expected, r.getGeneratedCode("A1")));
		}

	}

}
