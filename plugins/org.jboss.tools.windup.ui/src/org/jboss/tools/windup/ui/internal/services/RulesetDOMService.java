/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.windup.ui.internal.services;

import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IDocument;
import org.jboss.tools.windup.ui.internal.editor.RulesetWidgetFactory.JavaClassNodeConfig;
import org.jboss.tools.windup.ui.internal.editor.RulesetWidgetFactory.RuleNodeConfig;
import org.jboss.tools.windup.ui.internal.editor.RulesetWidgetFactory.RulesetConstants;
import org.jboss.tools.windup.ui.internal.editor.RulesetWidgetFactory.WhenNodeConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@SuppressWarnings("restriction")
@Creatable
@Singleton
public class RulesetDOMService {

	public Element findOrCreateRulesetElement(Document document) {
		NodeList nodeList = document.getElementsByTagName(RulesetConstants.RULESET_NAME);
		if (nodeList.getLength() > 0) {
			return (Element)nodeList.item(0);
		}
		Element rulesetElement = document.createElement(RulesetConstants.RULESET_NAME);
		document.appendChild(rulesetElement);
		return rulesetElement;
	}
	
	public Element findOrCreateRulesElement(Element rulesetElement) {
		NodeList nodeList = rulesetElement.getOwnerDocument().getElementsByTagName(RulesetConstants.RULES_NAME);
		if (nodeList.getLength() > 0) {
			return (Element)nodeList.item(0);
		}
		Element rulesElement = rulesetElement.getOwnerDocument().createElement(RulesetConstants.RULES_NAME);
		rulesetElement.appendChild(rulesetElement);
		return rulesElement;
	}
	
	public Element createRuleElement(Element rulesElement) {
		Element ruleElement = rulesElement.getOwnerDocument().createElement(RuleNodeConfig.NAME);
		generateNextRuleId(ruleElement);
		rulesElement.appendChild(ruleElement);
		return ruleElement;
	}
	
	public Element createJavaClassReferencesImportElement(String fullyQualifiedName, Element rulesetElement) {
		Element javaClassElement = createJavaClassElement(rulesetElement.getOwnerDocument());
		javaClassElement.setAttribute(RulesetConstants.JAVA_CLASS_REFERENCES, fullyQualifiedName);
		return javaClassElement;
	}
	
	public Element createPerformElement(Element ruleElement) {
		Element performElement = ruleElement.getOwnerDocument().createElement(RulesetConstants.PERFORM);
		ruleElement.appendChild(performElement);
		return performElement;
	}
	
	public Element createWhenElement(Document document) {
		Element whenElement = document.createElement(WhenNodeConfig.NAME);
		return whenElement;
	}
	
	public Element createJavaClassElement(Document document) {
		Element javaclassElement = document.createElement(JavaClassNodeConfig.NAME);
		return javaclassElement;
	}
	
	public Element createJavaClassReferenceLocation(Element javaClassElement) {
		Element locationElement = javaClassElement.getOwnerDocument().createElement(RulesetConstants.JAVA_CLASS_LOCATION);
		javaClassElement.appendChild(locationElement);
		return locationElement;
	}
	
	public ITypeRoot getEditorInput(JavaEditor editor) {
		return JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
	}
	
	public IDocument getDocument(JavaEditor editor) {
		return JavaUI.getDocumentProvider().getDocument(editor.getEditorInput());
	}
	
	public String getRulesetId(Document document) {
		String rulesetId = "";
		NodeList nodeList = document.getElementsByTagName(RulesetConstants.RULESET_NAME);
		if (nodeList.getLength() == 1) {
			Element element = (Element)nodeList.item(0);
			rulesetId = element.getAttribute(RulesetConstants.ID);
		}
		return rulesetId;
	}
	
	public void generateNextRuleId(Element ruleElement) {
		Element rulesetElement = findOrCreateRulesetElement(ruleElement.getOwnerDocument());
		String id = rulesetElement.getAttribute(RulesetConstants.ID);
		NodeList rules = rulesetElement.getElementsByTagName(RulesetConstants.RULE_NAME);
		id += "-" + rules.getLength();
		ruleElement.setAttribute(RulesetConstants.ID, id);
	}
}