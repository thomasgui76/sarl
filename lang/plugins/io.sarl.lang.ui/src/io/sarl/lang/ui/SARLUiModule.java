/*
 * generated by Xtext
 */
package io.sarl.lang.ui;

import io.sarl.lang.ui.custom.SARLHighlightingCalculator;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.xtext.ui.editor.syntaxcoloring.ISemanticHighlightingCalculator;

/**
 * Use this class to register components to be used within the IDE.
 */
public class SARLUiModule extends io.sarl.lang.ui.AbstractSARLUiModule {

	/**
	 * @param plugin
	 */
	public SARLUiModule(AbstractUIPlugin plugin) {
		super(plugin);
	}

	/** {@inheritDoc}
	 */
	@Override
	public Class<? extends ISemanticHighlightingCalculator> bindISemanticHighlightingCalculator() {
		return SARLHighlightingCalculator.class;
	}
}
