/*********************************************************************************************
 *
 * 'EditorListener.java, in plugin gama.ui.base, is part of the source code of the GAMA modeling and
 * simulation platform. (v. 1.8.1)
 *
 * (c) 2007-2020 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 * 
 *
 **********************************************************************************************/
package gama.ui.base.interfaces;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import gama.runtime.exceptions.GamaRuntimeException;

/**
 * Written by drogoul Modified on 27 mai 2011
 * 
 * @todo Description
 * 
 */
public interface EditorListener<T> {

	void valueModified(T val) throws GamaRuntimeException;

	public static interface Command extends EditorListener<Object>, SelectionListener {

		@Override
		public default void valueModified(final Object o) {
			this.widgetSelected(null);
		}

		@Override
		public default void widgetDefaultSelected(final SelectionEvent o) {
			this.widgetSelected(null);
		}

	}

}
