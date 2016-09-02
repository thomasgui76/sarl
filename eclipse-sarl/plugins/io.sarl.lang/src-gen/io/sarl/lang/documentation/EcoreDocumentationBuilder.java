/*
 * $Id$
 *
 * File is automatically generated by the Xtext language generator.
 * Do not change it.
 *
 * SARL is an general-purpose agent programming language.
 * More details on http://www.sarl.io
 *
 * Copyright 2014-2016 the original authors and authors.
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
package io.sarl.lang.documentation;

import io.sarl.lang.services.SARLGrammarAccess;
import javax.inject.Inject;
import org.eclipse.xtend.core.xtend.XtendMember;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Group;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.UntilToken;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.xbase.lib.Pure;

/** Build a documentation string.
 */
public class EcoreDocumentationBuilder implements IEcoreDocumentationBuilder {

	private AbstractRule mlRule;

	private AbstractRule slRule;

	private String mlStartSymbols;

	private String mlEndTagSymbols;

	private String slStartSymbols;

	@Inject
	private IDocumentationFormatter documentationFormatter;

	@Inject
	public void setGrammarAccess(SARLGrammarAccess access) {
		this.mlRule = access.getML_COMMENTRule();
		this.slRule = access.getSL_COMMENTRule();
		for (AbstractElement element : ((Group) this.mlRule.getAlternatives()).getElements()) {
			if (element instanceof Keyword && Strings.isEmpty(this.mlStartSymbols)) {
				this.mlStartSymbols = ((Keyword) element).getValue();
			} else if (element instanceof UntilToken && Strings.isEmpty(this.mlEndTagSymbols)) {
				this.mlEndTagSymbols = ((Keyword) ((UntilToken) element).getTerminal()).getValue();
			}
		}
		AbstractRule slRule = access.getSL_COMMENTRule();
		for (AbstractElement element : ((Group) slRule.getAlternatives()).getElements()) {
			if (element instanceof Keyword) {
				this.slStartSymbols = ((Keyword) element).getValue().trim();
				break;
			}
		}
	}

	@Pure
	public AbstractRule getMLCommentRule() {
		return this.mlRule;
	}

	@Pure
	public AbstractRule getSLCommentRule() {
		return this.slRule;
	}

	@Pure
	public IDocumentationFormatter getDocumentationFormatter() {
		return this.documentationFormatter;
	}

	protected boolean isMultilineCommentFor(Class<?> type) {
		return XtendMember.class.isAssignableFrom(type);
	}

	@Pure
	public String build(String doc, Class<?> objectType) {
		String givenDocumentation = Strings.emptyIfNull(doc).trim();
		StringBuilder documentation = new StringBuilder();
		IDocumentationFormatter formatter = getDocumentationFormatter();
		if (isMultilineCommentFor(objectType)) {
			if (!givenDocumentation.startsWith(this.mlStartSymbols)) {
				documentation.append(this.mlStartSymbols);
			}
			documentation.append(givenDocumentation);
			if (!givenDocumentation.endsWith(this.mlEndTagSymbols)) {
				documentation.append(this.mlEndTagSymbols);
			}
			return formatter.formatMultilineComment(documentation.toString());
		}
		documentation.append("\n");
		if (!givenDocumentation.startsWith(this.slStartSymbols)) {
			documentation.append(this.slStartSymbols);
		}
		documentation.append(givenDocumentation);
		if (!givenDocumentation.isEmpty() && !isNewLine(givenDocumentation.charAt(givenDocumentation.length() - 1))) {
			documentation.append("\n");
		}
		return formatter.formatSinglelineComment(documentation.toString());
	}

	private static boolean isNewLine(char character) {
		if (character == '\n' || character == '\r' || character == '\f') {
			return true;
		}
		return ((((1 << Character.LINE_SEPARATOR)
				| (1 << Character.PARAGRAPH_SEPARATOR)) >> Character.getType((int) character)) & 1) != 0;
	}

}
