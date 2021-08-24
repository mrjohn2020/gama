/*********************************************************************************************
 *
 * 'ExpressionControl.java, in plugin gama.ui.base, is part of the source code of the GAMA modeling and
 * simulation platform. (v. 1.8.1)
 *
 * (c) 2007-2020 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package gama.ui.base.parameters;

import static gama.ui.base.resources.GamaColors.get;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import gama.common.util.StringUtils;
import gama.kernel.simulation.SimulationAgent;
import gama.metamodel.agent.IAgent;
import gama.runtime.GAMA;
import gama.runtime.IScope;
import gama.runtime.exceptions.GamaRuntimeException;
import gama.ui.base.resources.GamaColors;
import gama.ui.base.utils.ThemeHelper;
import gaml.compilation.GAML;
import gaml.expressions.IExpression;
import gaml.types.GamaStringType;
import gaml.types.IType;
import gaml.types.Types;
import gama.ui.base.toolbar.GamaToolbarFactory;

@SuppressWarnings ({ "rawtypes", "unchecked" })
public class ExpressionControl implements /* IPopupProvider, */SelectionListener, ModifyListener, FocusListener {

	private final Text text;
	private final ExpressionBasedEditor<Object> editor;
	private Object currentValue;
	protected Exception currentException;
	final boolean evaluateExpression;
	private final IAgent hostAgent;
	private final IScope scope;
	private final IType<?> expectedType;
	MouseTrackListener tooltipListener = new MouseTrackAdapter() {

		@Override
		public void mouseExit(final MouseEvent arg0) {
			removeTooltip();
		}
	};

	public ExpressionControl(final IScope scope, final Composite comp, final ExpressionBasedEditor ed,
			final IAgent agent, final IType<?> expectedType, final int controlStyle, final boolean evaluate) {
		this.scope = scope;
		editor = ed;
		evaluateExpression = evaluate;
		hostAgent = agent;
		this.expectedType = expectedType;
		text = createTextBox(comp, controlStyle);
		text.addModifyListener(this);
		text.addFocusListener(this);
		text.addSelectionListener(this);
		text.addMouseTrackListener(tooltipListener);
		// if (ed != null) { ed.getLabel().getLabel().addMouseTrackListener(tooltipListener); }
	}

	@Override
	public void modifyText(final ModifyEvent event) {
		if (editor != null && editor.internalModification) return;
		modifyValue();
		displayTooltip();
	}

	protected void displayTooltip() {
		final var s = getPopupText();
		if (s == null || s.isEmpty()) {
			removeTooltip();
		} else {
			final var displayer = GamaToolbarFactory.findTooltipDisplayer(text);
			if (displayer != null) { displayer.displayTooltip(s, null); }
		}
		if (editor != null && currentException != null) { editor.getLabel().signalErrored(); }
	}

	protected void removeTooltip() {
		final var displayer = GamaToolbarFactory.findTooltipDisplayer(text);
		if (displayer != null) { displayer.stopDisplayingTooltips(); }
		if (editor != null) { editor.getLabel().cancelErrored(); }

	}

	@Override
	public void widgetDefaultSelected(final SelectionEvent me) {
		try {
			if (text == null || text.isDisposed()) return;
			modifyValue();
			displayValue(getCurrentValue());
		} catch (final RuntimeException e) {
			e.printStackTrace();
		}
	}

	private Object computeValue() {
		try {
			currentException = null;
			var agent = getHostAgent();
			// AD: fix for SWT Issue in Eclipse 4.4
			if (text == null || text.isDisposed()) return null;
			var s = text.getText();
			if (expectedType == Types.STRING && !StringUtils.isGamaString(s)) { s = StringUtils.toGamlString(s); }
			// AD: Fix for Issue 1042
			if (agent != null && (agent.getScope().interrupted() || agent.dead()) && agent instanceof SimulationAgent) {
				agent = agent.getScope().getExperiment();
				if (agent == null) { agent = GAMA.getRuntimeScope().getExperiment(); }
			}
			if (NumberEditor.UNDEFINED_LABEL.equals(s)) {
				setCurrentValue(null);
			} else if (agent == null) {
				if (expectedType == Types.STRING) {
					setCurrentValue(StringUtils.toJavaString(GamaStringType.staticCast(null, s, false)));
				} else {
					setCurrentValue(expectedType.cast(scope, s, null, false));
				}
			} else if (!agent.dead()) {
				// Solves Issue #3104 when the experiment agent dies
				setCurrentValue(evaluateExpression ? GAML.evaluateExpression(s, agent)
						: GAML.compileExpression(s, agent, true));
			}
		} catch (final Exception e) {
			currentException = e;
			return null;
		}
		return getCurrentValue();
	}

	public void modifyValue() {
		final var oldValue = getCurrentValue();
		final var value = computeValue();
		if (currentException != null) {
			setCurrentValue(oldValue);
			return;
		}
		if (editor != null) {
			try {

				if (editor.acceptNull && value == null) {
					editor.modifyValue(null);
				} else if (expectedType == Types.STRING) {
					editor.modifyValue(evaluateExpression
							? StringUtils.toJavaString(GamaStringType.staticCast(scope, value, false)) : value);
				} else {
					editor.modifyValue(evaluateExpression ? expectedType.cast(scope, value, false, false) : value);
				}
				editor.updateToolbar();

			} catch (final GamaRuntimeException e) {
				setCurrentValue(oldValue);
				currentException = e;
			}
		}
	}

	protected Text createTextBox(final Composite comp, final int controlStyle) {
		var c = new Composite(comp, SWT.NONE);
		var f = new FillLayout();
		f.marginHeight = 2;
		f.marginWidth = 2;
		c.setLayout(f);
		final var d = new GridData(SWT.FILL, SWT.CENTER, true, false);
		// d.heightHint = 20;
		c.setLayoutData(d);

		c.addListener(SWT.Paint, e -> {
			GC gc = e.gc;
			Rectangle bounds = c.getBounds();
			Color ref = comp.getBackground();
			gc.setBackground(ThemeHelper.isDark() ? get(ref).lighter() : get(ref).darker());
			// gc.setForeground(gc.getBackground());
			gc.fillRoundRectangle(0, 0, bounds.width, bounds.height, 5, 5);
		});
		final var t = new Text(c, controlStyle);
		t.setForeground(GamaColors.getTextColorForBackground(comp.getBackground()).color());

		// force the color, see #2601
		return t;
	}

	@Override
	public void focusGained(final FocusEvent e) {}

	@Override
	public void focusLost(final FocusEvent e) {
		if (e.widget == null || !e.widget.equals(text)) return;
		widgetDefaultSelected(null);
	}

	public Text getControl() {
		return text;
	}

	@Override
	public void widgetSelected(final SelectionEvent e) {}

	/**
	 * @see gama.ui.base.controls.IPopupProvider#getPopupText()
	 */
	public String getPopupText() {
		StringBuilder result = new StringBuilder();
		final var value = getCurrentValue();
		if (currentException != null) {
			result.append(currentException.getMessage());
		} else if (!isOK(value)) {
			result.append("The current value should be of type ").append(expectedType.toString());
		}
		return result.toString();
	}

	private Boolean isOK(final Object value) {
		if (evaluateExpression) return expectedType.canBeTypeOf(scope, value);
		if (value instanceof IExpression)
			return expectedType.isAssignableFrom(((IExpression) value).getGamlType());
		else
			return false;
	}

	IAgent getHostAgent() {
		return hostAgent == null ? editor == null ? null : editor.getAgent() : hostAgent;
	}

	/**
	 * @return the currentValue
	 */
	protected Object getCurrentValue() {
		return currentValue;
	}

	/**
	 * @param currentValue
	 *            the currentValue to set
	 */
	protected void setCurrentValue(final Object currentValue) {
		this.currentValue = currentValue;
	}

	/**
	 * @param currentValue2
	 */
	public void displayValue(final Object currentValue2) {
		setCurrentValue(evaluateExpression ? expectedType == Types.STRING
				? StringUtils.toJavaString(GamaStringType.staticCast(scope, currentValue2, false))
				: expectedType.cast(scope, currentValue2, null, false) : currentValue2);
		if (text.isDisposed()) return;
		if (expectedType == Types.STRING) {
			text.setText(currentValue == null ? "" : StringUtils.toJavaString(currentValue.toString()));
		} else {
			text.setText(StringUtils.toGaml(currentValue2, false));
		}
	}

}
