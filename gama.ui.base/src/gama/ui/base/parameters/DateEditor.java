/*********************************************************************************************
 *
 * 'DateEditor.java, in plugin gama.ui.base, is part of the source code of the GAMA modeling and simulation
 * platform. (v. 1.8.1)
 *
 * (c) 2007-2020 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package gama.ui.base.parameters;

import java.awt.Color;
import java.time.LocalDateTime;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;

import gama.kernel.experiment.IParameter;
import gama.kernel.experiment.InputParameter;
import gama.metamodel.agent.IAgent;
import gama.runtime.IScope;
import gama.ui.base.interfaces.EditorListener;
import gama.util.GamaDate;
import gaml.types.IType;
import gaml.types.Types;

public class DateEditor extends AbstractEditor<GamaDate> {

	private Composite edit;
	private DateTime date;
	private DateTime time;

	DateEditor(final IScope scope, final IAgent agent, final IParameter param, final EditorListener<GamaDate> l) {
		super(scope, agent, param, l);
	}

	DateEditor(final IScope scope, final EditorsGroup parent, final String title, final Object value,
			final EditorListener<GamaDate> whenModified) {
		super(scope, new InputParameter(title, value), whenModified);
		this.createControls(parent);
	}

	@Override
	public void widgetSelected(final SelectionEvent e) {
		modifyAndDisplayValue(GamaDate.of(LocalDateTime.of(date.getYear(), date.getMonth() + 1, date.getDay(),
				time.getHours(), time.getMinutes(), time.getSeconds())));
	}

	@Override
	public Control createCustomParameterControl(final Composite compo) {
		edit = new Composite(compo, SWT.NONE);
		final GridLayout pointEditorLayout = new GridLayout(2, true);
		pointEditorLayout.horizontalSpacing = 10;
		pointEditorLayout.verticalSpacing = 0;
		pointEditorLayout.marginHeight = 0;
		pointEditorLayout.marginWidth = 0;
		edit.setLayout(pointEditorLayout);
		date = new DateTime(edit, SWT.DROP_DOWN | SWT.DATE | SWT.LONG);
		time = new DateTime(edit, SWT.DROP_DOWN | SWT.TIME | SWT.LONG);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, true);
		date.setBackground(parent.getBackground());
		date.addSelectionListener(this);
		date.setLayoutData(data);
		time.setBackground(parent.getBackground());
		time.addSelectionListener(this);
		time.setLayoutData(GridDataFactory.copyData(data));
		edit.setBackground(parent.getBackground());
		displayParameterValue();
		return edit;
	}

	@Override
	protected void displayParameterValue() {
		internalModification = true;
		final GamaDate d = getCurrentValue();
		if (d != null) {
			date.setDate(d.getYear(), d.getMonth() - 1, d.getDay());
			time.setTime(d.getHour(), d.getMinute(), d.getSecond());
		}
		internalModification = false;
	}

	@Override
	public IType<Color> getExpectedType() {
		return Types.DATE;
	}

	@Override
	protected int[] getToolItems() {
		return new int[] { REVERT };
	}

}
