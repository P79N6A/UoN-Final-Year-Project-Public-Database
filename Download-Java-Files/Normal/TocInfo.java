/*******************************************************************************
 * Copyright (c) 2010 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.report.engine.emitter.odt;

public class TocInfo
{
	public String tocValue;
	public int tocLevel;

	TocInfo( String tocValue, int tocLevel )
	{
		this.tocValue = tocValue;
		this.tocLevel = tocLevel;
	}
}