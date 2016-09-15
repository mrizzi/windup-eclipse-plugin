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
package org.jboss.tools.windup.ui.internal.explorer;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.windup.ui.WindupUIPlugin;
import org.jboss.tools.windup.ui.internal.Messages;

/**
 * Dialog for viewing a difference.
 */
public class DiffDialog extends Dialog {
	
	private static final int WIDTH = 950;
	private static final int HEIGHT = 600;
	
	private ComparePreviewer viewer;
	
	protected IResource left;
	protected IResource right;
	
	public DiffDialog(Shell shell, IResource left, IResource right) {
		super(shell);
		this.left = left;
		this.right = right;
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(WIDTH, HEIGHT);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = doCreateDialogArea(parent);
		loadPreview(left, right);
		return control;
	}
	
	protected Control doCreateDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		viewer = new ComparePreviewer((Composite)control);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer.getViewer().getControl().getParent());
		return control;
	}
	
	protected void loadPreview(IResource left, IResource right) {
		try {
			String leftContents = FileUtils.readFileToString(left.getLocation().toFile());
			String rightContents = FileUtils.readFileToString(right.getLocation().toFile());
			viewer.setInput(new DiffNode(
					new CompareElement(leftContents, left.getFileExtension(), left),
					new CompareElement(rightContents, left.getFileExtension(), right)));
		} catch (IOException e) {
			WindupUIPlugin.log(e);
		}
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	private static class ComparePreviewer extends CompareViewerSwitchingPane {
		private CompareConfiguration fCompareConfiguration;
		public ComparePreviewer(Composite parent) {
			super(parent, SWT.BORDER | SWT.FLAT, true);
			fCompareConfiguration= new CompareConfiguration();
			fCompareConfiguration.setLeftEditable(false);
			fCompareConfiguration.setLeftLabel(Messages.ComparePreviewer_original_source);
			fCompareConfiguration.setRightEditable(false);
			fCompareConfiguration.setRightLabel(Messages.ComparePreviewer_migrated_source);
			Dialog.applyDialogFont(this);
		}
		@Override
		protected Viewer getViewer(Viewer oldViewer, Object input) {
			return CompareUI.findContentViewer(oldViewer, (ICompareInput)input, this, fCompareConfiguration);
		}
		@Override
		public void setText(String text) {
			super.setText(text);
			setImage(WindupUIPlugin.getDefault().getImageRegistry().get(WindupUIPlugin.IMG_WINDUP));
		}
	}
	
	private static class CompareElement implements ITypedElement, IEncodedStreamContentAccessor, IResourceProvider {

		private static final String ENCODING = "UTF-8";	//$NON-NLS-1$
		
		private String content;
		private String type;
		private IResource resource;
		
		public CompareElement(String content, String type, IResource resource) {
			this.content = content;
			this.type = type;
			this.resource = resource;
		}
		@Override
		public String getName() {
			return "";
		}
		@Override
		public Image getImage() {
			return null;
		}
		@Override
		public String getType() {
			return type;
		}
		@Override
		public InputStream getContents() throws CoreException {
			try {
				return new ByteArrayInputStream(content.getBytes(ENCODING));
			} catch (UnsupportedEncodingException e) {
				return new ByteArrayInputStream(content.getBytes());
			}
		}
		@Override
		public String getCharset() {
			return ENCODING;
		}
		@Override
		public IResource getResource() {
			return resource;
		}
	}
	
	public static class QuickFixTempProject {
				
		private static final String TMP_PROJECT_NAME = ".org.jboss.toos.windup.compare.tmp"; //$NON-NLS-1$
		
		private final static String TMP_PROJECT_FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
				+ "<projectDescription>\n" //$NON-NLS-1$
				+ "\t<name>" + TMP_PROJECT_NAME + "\t</name>\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "\t<comment></comment>\n" //$NON-NLS-1$
				+ "\t<projects>\n" //$NON-NLS-1$
				+ "\t</projects>\n" //$NON-NLS-1$
				+ "\t<buildSpec>\n" //$NON-NLS-1$
				+ "\t</buildSpec>\n" //$NON-NLS-1$
				+ "\t<natures>\n" + "\t</natures>\n" //$NON-NLS-1$//$NON-NLS-2$
				+ "</projectDescription>"; //$NON-NLS-1$
		
		private final static String TMP_FOLDER_NAME = "tmpFolder"; //$NON-NLS-1$
		private final static String TMP_FILE_NAME = "tmpFile"; //$NON-NLS-1$
		
		private IProject createTmpProject() throws CoreException {
			IProject project = getTmpProject();
			if (!project.isAccessible()) {
				try {
					IPath stateLocation = WindupUIPlugin.getDefault().getStateLocation();
					if (!project.exists()) {
						IProjectDescription desc = project.getWorkspace()
								.newProjectDescription(project.getName());
						desc.setLocation(stateLocation.append(TMP_PROJECT_NAME));
						project.create(desc, null);
					}
					try {
						project.open(null);
					} catch (CoreException e) { // in case .project file or folder has been deleted
						IPath projectPath = stateLocation.append(TMP_PROJECT_NAME);
						projectPath.toFile().mkdirs();
						FileOutputStream output = new FileOutputStream(
								projectPath.append(".project").toOSString()); //$NON-NLS-1$
						try {
							output.write(TMP_PROJECT_FILE.getBytes());
						} finally {
							output.close();
						}
						project.open(null);
					}
					getTmpFolder(project);
				} catch (IOException ioe) {
					return project;
				} catch (CoreException ce) {
					throw new CoreException(ce.getStatus());
				}
			}
			if (!project.isOpen()) {
				project.open(null);
			}
			project.setHidden(true);
			return project;
		}
		
		private IProject getTmpProject() {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(TMP_PROJECT_NAME);
		}
		
		private IFolder getTmpFolder(IProject project) throws CoreException {
			IFolder folder = project.getFolder(TMP_FOLDER_NAME);
			if (!folder.exists())
				folder.create(IResource.NONE, true, null);
			return folder;
		}
		
		public IResource createResource(String contents) {
			try {
				IProject project = createTmpProject();
				IFolder folder = getTmpFolder(project);
				IResource resource = folder.getFile(TMP_FILE_NAME);
				InputStream input = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
				if (!resource.exists()) {
					((IFile)resource).create(input, true, new NullProgressMonitor());
				}
				else {
					((IFile)resource).setContents(input, IResource.FORCE, new NullProgressMonitor());
				}
				return resource;
			} catch (CoreException e) {
				WindupUIPlugin.log(e);
				MessageDialog.openError(Display.getDefault().getActiveShell(),
								Messages.ComparePreviewer_errorTitle,
								Messages.ComparePreviewer_errorMessage);
			}
			return null;
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IssueConstants.OK, Messages.ComparePreviewer_donePreviewFix, true);
		Button button = createButton(parent, IssueConstants.APPLY_FIX, Messages.ComparePreviewer_applyFix, false);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setReturnCode(IssueConstants.APPLY_FIX);
				close();
			}
		});
	}
	
	public IResource getRight() {
		return right;
	}
}
