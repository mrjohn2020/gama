/*********************************************************************************************
 *
 * 'ListEditor.java, in plugin gama.ui.base, is part of the source code of the GAMA modeling and simulation
 * platform. (v. 1.8.1)
 *
 * (c) 2007-2020 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package gama.ui.base.parameters;

import org.eclipse.jface.dialogs.IDialogConstants;

import gama.kernel.experiment.IParameter;
import gama.metamodel.agent.IAgent;
import gama.runtime.IScope;
import gama.ui.base.interfaces.EditorListener;
import gama.ui.base.utils.WorkbenchHelper;
import gama.util.IList;
import gaml.types.IType;
import gaml.types.Types;

public class ListEditor extends ExpressionBasedEditor<java.util.List<?>> {

	ListEditor(final IScope scope, final IAgent agent, final IParameter param,
			final EditorListener<java.util.List<?>> l) {
		super(scope, agent, param, l);
	}

	@SuppressWarnings ("rawtypes")
	@Override
	public void applyEdit() {
		if (currentValue instanceof IList) {
			final ListEditorDialog d =
					new ListEditorDialog(WorkbenchHelper.getShell(), (IList) currentValue, param.getName());
			if (d.open() == IDialogConstants.OK_ID) { modifyAndDisplayValue(d.getList(ListEditor.this)); }
		}
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();
		editorToolbar.enable(EDIT, currentValue instanceof IList);
	}

	@SuppressWarnings ({ "unchecked", "rawtypes" })
	@Override
	public IType getExpectedType() {
		return Types.LIST;
	}

	@Override
	protected int[] getToolItems() {
		return new int[] { EDIT, REVERT };
	}

}
