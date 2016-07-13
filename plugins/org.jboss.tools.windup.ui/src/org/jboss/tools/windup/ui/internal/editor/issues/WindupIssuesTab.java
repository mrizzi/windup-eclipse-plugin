/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.windup.ui.internal.editor.issues;

import static org.jboss.tools.windup.ui.internal.Messages.issuesTab;

import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.windup.ui.internal.editor.WindupFormTab;

/**
 * Windup's migration issues tab.
 */
public class WindupIssuesTab extends WindupFormTab {
	
	@Override
	protected void createFormContent(Composite parent) {
		item.setText(issuesTab);
	}
}