package ummisco.gama.ui.parameters;

import static msi.gama.application.workbench.ThemeHelper.isDark;
import static msi.gama.common.util.StringUtils.toGaml;
import static ummisco.gama.ui.resources.IGamaColors.BLACK;
import static ummisco.gama.ui.resources.IGamaColors.VERY_LIGHT_GRAY;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;

public class FixedValueEditorControl extends EditorControl<CLabel> {

	/**
	 * Constructor for building a read-only value control
	 *
	 * @param value
	 *            the original value to display
	 */
	FixedValueEditorControl(final AbstractEditor editor, final Composite parent) {
		super(editor, new CLabel(parent, SWT.READ_ONLY));
		setForeground(isDark() ? VERY_LIGHT_GRAY.color() : BLACK.color());
		// force text color, see #2601
	}

	@Override
	public void setText(final String s) {
		if (control.isDisposed()) return;
		control.setText(s);
	}

	@Override
	public void displayParameterValue() {
		Object val = editor.getCurrentValue();
		setText(val instanceof String ? (String) val : toGaml(val, false));
	}

}
