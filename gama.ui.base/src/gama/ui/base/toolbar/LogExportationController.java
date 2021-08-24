/*********************************************************************************************
 *
 * 'CSVExportationController.java, in plugin gama.ui.base, is part of the source code of the GAMA modeling and
 * simulation platform. (v. 1.8.1)
 *
 * (c) 2007-2020 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package gama.ui.base.toolbar;

import org.eclipse.swt.SWT;

import gama.ui.base.resources.GamaIcons;
import gama.ui.base.resources.IGamaIcons;
import gama.ui.base.toolbar.IToolbarDecoratedView.LogExportable;

/**
 * Class ZoomController.
 *
 * @author drogoul
 * @since 9 févr. 2015
 *
 */
public class LogExportationController {

	private final IToolbarDecoratedView.LogExportable view;

	/**
	 * @param view2
	 */
	public LogExportationController(final LogExportable view2) {
		this.view = view2;
	}

	/**
	 * @param tb
	 */
	public void install(final GamaToolbar2 tb) {
		tb.button(GamaIcons.create(IGamaIcons.DISPLAY_TOOLBAR_CSVEXPORT).getCode(), "Export to log file",
				"Export to log file", e -> view.saveAsLog(), SWT.RIGHT);

	}

}
