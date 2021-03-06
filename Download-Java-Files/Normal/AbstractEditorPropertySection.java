/**
 * Copyright (c) 2011 committers of YAKINDU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * 	committers of YAKINDU - initial API and implementation
 * 
 */
package org.yakindu.sct.ui.editor.propertysheets;

import org.eclipse.emf.databinding.EMFDataBindingContext;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gmf.runtime.diagram.ui.properties.sections.AbstractModelerPropertySection;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogLabelKeys;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.xtext.EcoreUtil2;
import org.yakindu.base.base.DomainElement;
import org.yakindu.base.xtext.utils.jface.fieldassist.CompletionProposalAdapter;
import org.yakindu.base.xtext.utils.jface.viewers.FilteringMenuManager;
import org.yakindu.base.xtext.utils.jface.viewers.StyledTextXtextAdapter;
import org.yakindu.base.xtext.utils.jface.viewers.util.ActiveEditorTracker;
import org.yakindu.sct.domain.extension.DomainRegistry;
import org.yakindu.sct.domain.extension.DomainStatus;
import org.yakindu.sct.domain.extension.IDomain;
import org.yakindu.sct.domain.extension.DomainStatus.Severity;
import org.yakindu.sct.model.sgraph.util.ContextElementAdapter;
import org.yakindu.sct.ui.editor.editor.StatechartDiagramEditor;

import com.google.inject.Injector;

/**
 * 
 * @author andreas muelder - Initial contribution and API
 * 
 */
public abstract class AbstractEditorPropertySection extends AbstractModelerPropertySection {

	public abstract void createControls(Composite parent);

	public abstract void bindModel(EMFDataBindingContext context);

	private FormToolkit toolkit;

	private EMFDataBindingContext bindingContext;

	private Form form;

	private CompletionProposalAdapter completionProposalAdapter;

	public final static String CONTEXTMENUID = "org.yakindu.base.xtext.utils.jface.viewers.StyledTextXtextAdapterContextMenu";

	@Override
	public void refresh() {
		super.refresh();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (bindingContext != null)
			bindingContext.dispose();
		if (toolkit != null)
			toolkit.dispose();
	}

	@Override
	protected void setEObject(EObject newValue) {
		EObject oldValue = getEObject();
		super.setEObject(newValue);
		if (newValue != null && newValue != oldValue) {
			inputChanged();
		}
	}

	protected void inputChanged() {
		if (bindingContext != null)
			bindingContext.dispose();
		bindingContext = new ValidatingEMFDatabindingContext(getEObject(), form.getShell());
		bindModel(bindingContext);
	}

	@Override
	public final void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		toolkit = new FormToolkit(parent.getDisplay());
		toolkit.setBorderStyle(SWT.BORDER);
		super.createControls(parent, aTabbedPropertySheetPage);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		parent.setLayout(new GridLayout(1, true));
		form = toolkit.createForm(parent);
		toolkit.decorateFormHeading(form);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		form.getBody().setLayout(createBodyLayout());
		createControls(form.getBody());
	}

	protected Layout createBodyLayout() {
		return new GridLayout(1, false);
	}

	public FormToolkit getToolkit() {
		return toolkit;
	}

	protected void enableXtext(Control styledText, Injector injector) {
		final StyledTextXtextAdapter xtextAdapter = new StyledTextXtextAdapter(injector);
		xtextAdapter.getFakeResourceContext().getFakeResource().eAdapters()
				.add(new ContextElementAdapter(getEObject()));
		xtextAdapter.adapt((StyledText) styledText);

		initContextMenu(styledText);

		completionProposalAdapter = new CompletionProposalAdapter(styledText, xtextAdapter.getContentAssistant(),
				KeyStroke.getInstance(SWT.CTRL, SWT.SPACE), null);
		
		form.getBody().setEnabled(isEditable());
	}

	protected void initContextMenu(Control control) {
		MenuManager menuManager = new FilteringMenuManager();
		Menu contextMenu = menuManager.createContextMenu(control);
		control.setMenu(contextMenu);
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPartSite site = window.getActivePage().getActiveEditor().getSite();
		site.registerContextMenu(CONTEXTMENUID, menuManager, site.getSelectionProvider());
	}

	public CompletionProposalAdapter getCompletionProposalAdapter() {
		return completionProposalAdapter;
	}

	protected Injector getInjector(String semanticTarget) {
		IEditorPart editor = ActiveEditorTracker.getLastActiveEditor();
		if (editor instanceof StatechartDiagramEditor) {
			IDomain domain = DomainRegistry.getDomain(((StatechartDiagramEditor) editor).getDiagram().getElement());
			return domain.getInjector(IDomain.FEATURE_EDITOR, semanticTarget);
		}
		throw new IllegalArgumentException("Unknown editor " + editor);

	}

	public EObject getContextObject() {
		return getEObject();
	}

	protected void createHelpWidget(final Composite parent, final Control control, String helpId) {
		final ImageHyperlink helpWidget = toolkit.createImageHyperlink(parent, SWT.CENTER);
		Image defaultImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_LCL_LINKTO_HELP);
		helpWidget.setImage(defaultImage);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(helpWidget);
		helpWidget.setToolTipText(JFaceResources.getString(IDialogLabelKeys.HELP_LABEL_KEY));
		helpWidget.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				control.setFocus();
				PlatformUI.getWorkbench().getHelpSystem().displayDynamicHelp();
			}
		});
		GridDataFactory.fillDefaults().applyTo(helpWidget);
		helpWidget.setEnabled(true);
		setHelpContext(control, helpId);
	}

	protected void setHelpContext(Control control, String helpId) {
		IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
		helpSystem.setHelp(control, helpId);
	}

	public boolean isEditable() {
		DomainStatus domainStatus = getDomainStatus();
		if (domainStatus == null || domainStatus.getSeverity() == Severity.ERROR) {
			return false;
		}
		return true;
	}

	protected DomainStatus getDomainStatus() {
		EObject element = getEObject();
		DomainElement domainElement = EcoreUtil2.getContainerOfType(element, DomainElement.class);
		if (domainElement != null) {
			DomainStatus domainStatus = DomainRegistry.getDomainStatus(domainElement.getDomainID());
			return domainStatus;
		}
		return null;
	}

}
