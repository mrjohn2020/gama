/*********************************************************************************************
 *
 * 'MenuAction.java, in plugin gama.ui.base, is part of the source code of the GAMA modeling and simulation
 * platform. (v. 1.8.1)
 *
 * (c) 2007-2020 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 * 
 *
 **********************************************************************************************/
package gama.ui.base.menus;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;

public class MenuAction {

	public SelectionListener listener;
	public Image image;
	public String text;

	public MenuAction(final SelectionListener listener, final Image image, final String text) {
		super();
		this.listener = listener;
		this.image = image;
		this.text = text;
	}

}