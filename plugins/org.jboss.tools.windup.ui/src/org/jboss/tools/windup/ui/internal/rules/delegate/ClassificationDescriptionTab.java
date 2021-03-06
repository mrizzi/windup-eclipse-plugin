package org.jboss.tools.windup.ui.internal.rules.delegate;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.URLHyperlink;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.mylyn.internal.wikitext.ui.WikiTextUiPlugin;
import org.eclipse.mylyn.internal.wikitext.ui.editor.MarkupProjectionViewer;
import org.eclipse.mylyn.internal.wikitext.ui.editor.syntax.FastMarkupPartitioner;
import org.eclipse.mylyn.internal.wikitext.ui.editor.syntax.MarkupDocumentProvider;
import org.eclipse.mylyn.wikitext.markdown.MarkdownLanguage;
import org.eclipse.mylyn.wikitext.parser.Attributes;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.parser.markup.AbstractMarkupLanguage;
import org.eclipse.mylyn.wikitext.ui.editor.MarkupSourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.jboss.tools.windup.ui.WindupUIPlugin;
import org.jboss.tools.windup.ui.internal.Messages;
import org.jboss.tools.windup.ui.internal.editor.AddNodeAction;
import org.jboss.tools.windup.ui.internal.editor.ElementAttributesContainer;
import org.jboss.tools.windup.ui.internal.editor.RulesetElementUiDelegateFactory.RulesetConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Objects;

@SuppressWarnings("restriction")
public class ClassificationDescriptionTab extends ElementAttributesContainer {
	
	protected static final int VERTICAL_RULER_WIDTH = 12;
	
	private static final String CSS_CLASS_EDITOR_PREVIEW = "editorPreview"; //$NON-NLS-1$
	
	private Browser browser;
	private IDocument document;
	
	private SourceViewer sourceViewer;
	private MarkdownLanguage language = new MarkdownLanguage();
	
	private IHandlerService handlerService;
	private IHandlerActivation contentAssistHandlerActivation;
	
	private Section sourceSection;
	private Section previewSection;
	
	@PostConstruct
	public void createControls(Composite parent) {
		handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
		
		CTabFolder folder = createDescriptionSection(parent);
		createSourceTab(folder);
		CTabItem previewItem = createPreviewTab(folder);
		folder.setSelection(previewItem);
		
		updatePreview();
	}
	
	private CTabFolder createDescriptionSection(Composite parent) {
		parent = toolkit.createComposite(parent);
		GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 5).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);

		ColorRegistry reg = JFaceResources.getColorRegistry();
		Color c1 = reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_START"), //$NON-NLS-1$
			  c2 = reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_END"); //$NON-NLS-1$
		CTabFolder folder = new CTabFolder(parent, SWT.NO_REDRAW_RESIZE | SWT.FLAT);
		folder.setSelectionBackground(new Color[] {c1, c2},	new int[] {100}, true);
		folder.setSelectionForeground(reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_TEXT_COLOR")); //$NON-NLS-1$
		folder.setSimple(PlatformUI.getPreferenceStore().getBoolean(IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS));
		folder.setBorderVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(folder);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		folder.setLayoutData(gd);
		folder.setFont(parent.getFont());
		return folder;
	}
	
	private CTabItem createSourceTab(CTabFolder folder) {
		CTabItem item = new CTabItem(folder, SWT.BORDER);
		item.setText(Messages.RulesetEditor_descriptionSection);
		item.setImage(WindupUIPlugin.getDefault().getImageRegistry().get(WindupUIPlugin.IMG_MARKDOWN));
		Composite client = toolkit.createComposite(folder);
		GridLayoutFactory.fillDefaults().applyTo(client);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(client);
		item.setControl(client);
		createSourceViewer(client);
		return item;
	}
	
	private CTabItem createPreviewTab(CTabFolder folder) {
		CTabItem item = new CTabItem(folder, SWT.BORDER);
		item.setText(Messages.RulesetEditor_descriptionPreviewSection);
		item.setImage(WindupUIPlugin.getDefault().getImageRegistry().get(WindupUIPlugin.IMG_REPORT));
		Composite client = toolkit.createComposite(folder);
		GridLayoutFactory.fillDefaults().applyTo(client);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(client);
		item.setControl(client);
		createBrowser(client);
		return item;	
	}
	
	@Override
	protected void bind() {
		String message = getElementDescription();
		if (!Objects.equal(document.get(), message)) {
			document.set(getElementDescription());
		}
		sourceViewer.invalidateTextPresentation();
		updatePreview();
	}
	
	public Section getSourceEditorContainer() {
		return sourceSection;
	}
	
	public Section getBrowserContainer() {
		return previewSection;
	}
	
	private void createBrowser(Composite parent) {
		browser = new Browser(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 150).applyTo(browser);
		browser.addLocationListener(new LocationListener() {
			public void changed(LocationEvent event) {
				event.doit = false;
			}
			public void changing(LocationEvent event) {
				// if it looks like an absolute URL
				if (event.location.matches("([a-zA-Z]{3,8})://?.*")) { //$NON-NLS-1$
					// workaround for browser problem (bug 262043)
					int idxOfSlashHash = event.location.indexOf("/#"); //$NON-NLS-1$
					if (idxOfSlashHash != -1) {
						// allow javascript-based scrolling to work
						if (!event.location.startsWith("file:///#")) { //$NON-NLS-1$
							event.doit = false;
						}
						return;
					}
					// workaround end
					event.doit = false;
					try {
						IWebBrowser webBrowser = PlatformUI.getWorkbench()
								.getBrowserSupport()
								.createBrowser("org.eclipse.ui.browser"); //$NON-NLS-1$
						webBrowser.openURL(new URL(event.location));
					} catch (Exception e) {
						new URLHyperlink(new Region(0, 1), event.location).open();
					}
				}
			}
		});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private CMElementDeclaration getDescriptionCmNode() {
		List candidates = modelQuery.getAvailableContent(element, elementDeclaration, 
				ModelQuery.VALIDITY_STRICT);
		Optional<CMElementDeclaration> found = candidates.stream().filter(candidate -> {
			if (candidate instanceof CMElementDeclaration) {
				return RulesetConstants.DESCRIPTION.equals(((CMElementDeclaration)candidate).getElementName());
			}
			return false;
		}).findFirst();
		if (found.isPresent()) {
			return found.get();
		}
		return null;
	} 
	
	private Node getDescriptionNode() {
		NodeList list = element.getElementsByTagName(RulesetConstants.DESCRIPTION);
		if (list.getLength() > 0) {
			return list.item(0);
		}
		return null;
	}
	
	private String getElementDescription() {
		String message = "";
		Element node = (Element)getDescriptionNode();
		if (node != null) {
			message = getCdataMessage(node);
			if (message == null) {
				message = contentHelper.getNodeValue(node);
			}
		}
		return message == null ? "" : message;
	}
	
	private String getCdataMessage(Node messageNode) {
		String message = null;
		Node cdataNode = getCdataNode(messageNode);
		if (cdataNode != null) {
			message = contentHelper.getNodeValue(cdataNode);
		}
		return message;
	}

	private Node getCdataNode(Node messageNode) {
		NodeList childList = messageNode.getChildNodes();
		for (int i = 0; i < childList.getLength(); i++) {
			Node child = childList.item(i);
			if (Objects.equal(child.getNodeName(), RulesetConstants.CDATA)) {
				return child;
			}
		}
		return null;
	}
	
	protected void setMessage(String value) {
		Node node = getDescriptionNode();
		CMNode cmNode = getDescriptionCmNode();
		try {
			model.aboutToChangeModel();
			if (node != null) {
				Node cdataNode = getCdataNode(node);
				String currentDescription = contentHelper.getNodeValue(node);
				if (currentDescription != null && !currentDescription.isEmpty()) {
					contentHelper.setNodeValue(node, value);
				}
				else if (cdataNode != null) {
					contentHelper.setNodeValue(cdataNode, value);
				}
				else {
					createCdataNode(node, value);
				}
			}
			else {
				AddNodeAction newNodeAction = new AddNodeAction(model, cmNode, element, element.getChildNodes().getLength());
				newNodeAction.runWithoutTransaction();
				List<Node> newNodes = newNodeAction.getResult();
				if (!newNodes.isEmpty()) {
					node = newNodes.get(0);
					contentHelper.setNodeValue(node, "");
					createCdataNode(node, value);
				}
			}
		}
		finally {
			model.changedModel();
		}
	}
	
	private void createCdataNode(Node messageNode, String value) {
		Node cdataNode = messageNode.getOwnerDocument().createCDATASection(value);
		messageNode.appendChild(cdataNode);
	}
	
	protected void createSourceViewer(Composite parent) {
		String string = getElementDescription();
		IStorage storage = new StringInput.StringStorage(string);
		IEditorInput editorInput = new StringInput(storage);
		CompositeRuler ruler = new CompositeRuler();
		ISharedTextColors colors = EditorsPlugin.getDefault().getSharedTextColors();
		int styles = SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.WRAP;
		IOverviewRuler overviewRuler = new OverviewRuler(new DefaultMarkerAnnotationAccess(), VERTICAL_RULER_WIDTH, colors);
		
		this.sourceViewer = new MarkupProjectionViewer(parent, ruler, overviewRuler, true, styles);
		
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 100).applyTo(sourceViewer.getControl());
		
		try {
			MarkupDocumentProvider documentProvider = new MarkupDocumentProvider();
			documentProvider.connect(editorInput);
			this.document = documentProvider.getDocument(editorInput);
			//sourceViewer.setDocument(document);
			
			((AbstractMarkupLanguage) language).setEnableMacros(false);
			documentProvider.setMarkupLanguage(language);
			
			MarkupSourceViewerConfiguration sourceViewerConfiguration = new MarkupSourceViewerConfiguration(WikiTextUiPlugin.getDefault().getPreferenceStore());
			sourceViewerConfiguration.initializeDefaultFonts();
			sourceViewerConfiguration.setMarkupLanguage(language);
			sourceViewer.configure(sourceViewerConfiguration);
			
			IDocumentPartitioner partitioner = document.getDocumentPartitioner();
			FastMarkupPartitioner fastMarkupPartitioner = (FastMarkupPartitioner) partitioner;
			fastMarkupPartitioner.setMarkupLanguage(language);
		} catch (CoreException e) {
			WindupUIPlugin.log(e);
		}
		
		sourceViewer.getTextWidget().addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				deactivateEditorHandlers();
			}
			@Override
			public void focusGained(FocusEvent e) {
				activatEditorHandlers();
			}
		});
		
		IDocumentListener documentListener = new IDocumentListener() {
			public void documentAboutToBeChanged(DocumentEvent event) {}
			public void documentChanged(DocumentEvent event) {
				if (!blockNotification) {
					setMessage(document.get());
					updatePreview();
				}
			}
		};
		
		sourceViewer.getTextWidget().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				document.removeDocumentListener(documentListener);
				deactivateEditorHandlers();
			}
		});
		
		document.addDocumentListener(documentListener);
		
		configureAsEditor(sourceViewer, (Document)document);
	}
	
	private void configureAsEditor(ISourceViewer viewer, Document document) {
		IAnnotationAccess annotationAccess = new DefaultMarkerAnnotationAccess();
		final SourceViewerDecorationSupport support = new SourceViewerDecorationSupport(viewer, null, annotationAccess,
				EditorsUI.getSharedTextColors());
		Iterator<?> e = new MarkerAnnotationPreferences().getAnnotationPreferences().iterator();
		while (e.hasNext()) {
			support.setAnnotationPreference((AnnotationPreference) e.next());
		}
		support.install(EditorsUI.getPreferenceStore());
		viewer.getTextWidget().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				support.uninstall();
			}
		});
		AnnotationModel annotationModel = new AnnotationModel();
		viewer.setDocument(document, annotationModel);
	}
	
	private void activatEditorHandlers() {
		contentAssistHandlerActivation = handlerService.activateHandler(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, 
				new ActionHandler(new AssistAction(sourceViewer)));
	}
	
	private void deactivateEditorHandlers() {
		if (contentAssistHandlerActivation != null) {
			handlerService.deactivateHandler(contentAssistHandlerActivation);
			contentAssistHandlerActivation = null;
		}
	}
	
	private static class AssistAction extends Action {
		private ITextOperationTarget fOperationTarget;

		public AssistAction(SourceViewer sourceViewer) {
			this.fOperationTarget = sourceViewer.getTextOperationTarget();
		}
		
		@Override
		public void run() {
			BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
				@Override
				public void run() {
					fOperationTarget.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
				}
			});
		}
	}
	
	/**
	 * JavaScript that returns the current top scroll position of the browser widget
	 */
	private static final String JAVASCRIPT_GETSCROLLTOP = "function getScrollTop() { " //$NON-NLS-1$
			+ "  if(typeof pageYOffset!='undefined') return pageYOffset;" //$NON-NLS-1$
			+ "  else{" + //$NON-NLS-1$
			"var B=document.body;" + //$NON-NLS-1$
			"var D=document.documentElement;" + //$NON-NLS-1$
			"D=(D.clientHeight)?D:B;return D.scrollTop;}" //$NON-NLS-1$
			+ "}; return getScrollTop();"; //$NON-NLS-1$
	
	private void updatePreview() {
		Object result = browser.evaluate(JAVASCRIPT_GETSCROLLTOP);
		final int verticalScrollbarPos = result != null ? ((Number) result).intValue() : 0;
		String title = file == null ? "" : file.getName(); //$NON-NLS-1$
		if (title.lastIndexOf('.') != -1) {
			title = title.substring(0, title.lastIndexOf('.'));
		}
		StringWriter writer = new StringWriter();
		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer) {
			@Override
			protected void emitAnchorHref(String href) {
				if (href != null && href.startsWith("#")) { //$NON-NLS-1$
					writer.writeAttribute("onclick", //$NON-NLS-1$
							String.format("javascript: window.location.hash = '%s'; return false;", href)); //$NON-NLS-1$
					writer.writeAttribute("href", "#"); //$NON-NLS-1$//$NON-NLS-2$
				} else {
					super.emitAnchorHref(href);
				}
			}

			@Override
			public void beginHeading(int level, Attributes attributes) {
				attributes.appendCssClass(CSS_CLASS_EDITOR_PREVIEW);
				super.beginHeading(level, attributes);
			}

			@Override
			public void beginBlock(BlockType type, Attributes attributes) {
				attributes.appendCssClass(CSS_CLASS_EDITOR_PREVIEW);
				super.beginBlock(type, attributes);
			}
		};
		builder.setTitle(title);

		String css = WikiTextUiPlugin.getDefault().getPreferences().getMarkupViewerCss();
		builder.addCssStylesheet(new HtmlDocumentBuilder.Stylesheet(new StringReader(css)));

		AbstractMarkupLanguage markupLanguage = (AbstractMarkupLanguage)language.clone();
		markupLanguage.setEnableMacros(true);

		markupLanguage.setFilterGenerativeContents(false);
		markupLanguage.setBlocksOnly(false);

		MarkupParser markupParser = new MarkupParser();
		markupParser.setBuilder(builder);
		markupParser.setMarkupLanguage(markupLanguage);

		markupParser.parse(document.get());
		browser.addProgressListener(new ProgressAdapter() {
			@Override
			public void completed(ProgressEvent event) {
				browser.removeProgressListener(this);
				browser.execute(String.format("window.scrollTo(0,%d);", verticalScrollbarPos)); //$NON-NLS-1$
			}
		});
		String xhtml = writer.toString();
		browser.setText(xhtml);
	}
}