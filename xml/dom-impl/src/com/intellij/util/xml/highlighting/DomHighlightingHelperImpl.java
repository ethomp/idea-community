/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.util.xml.highlighting;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.ide.IdeBundle;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.*;
import com.intellij.util.xml.impl.*;
import com.intellij.util.xml.reflect.AbstractDomChildrenDescription;
import com.intellij.util.xml.reflect.DomCollectionChildDescription;
import com.intellij.util.xml.reflect.DomGenericInfo;
import com.intellij.xml.XmlBundle;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class DomHighlightingHelperImpl extends DomHighlightingHelper {
  private final GenericValueReferenceProvider myProvider = new GenericValueReferenceProvider();
  private final DomElementAnnotationsManagerImpl myAnnotationsManager;

  public DomHighlightingHelperImpl(final DomElementAnnotationsManagerImpl annotationsManager) {
    myAnnotationsManager = annotationsManager;
  }

  public void runAnnotators(DomElement element, DomElementAnnotationHolder holder, Class<? extends DomElement> rootClass) {
    myAnnotationsManager.annotate(element, holder, rootClass);
  }

  @NotNull
  public List<DomElementProblemDescriptor> checkRequired(final DomElement element, final DomElementAnnotationHolder holder) {
    final Required required = element.getAnnotation(Required.class);
    if (required != null) {
      final XmlElement xmlElement = element.getXmlElement();
      if (xmlElement == null) {
        if (required.value()) {
          final String xmlElementName = element.getXmlElementName();
          if (element instanceof GenericAttributeValue) {
            return Arrays.asList(holder.createProblem(element, IdeBundle.message("attribute.0.should.be.defined", xmlElementName)));
          }
          return Arrays.asList(
            holder.createProblem(
              element,
              HighlightSeverity.ERROR,
              IdeBundle.message("child.tag.0.should.be.defined", xmlElementName),
              new AddRequiredSubtagFix(xmlElementName, element.getXmlElementNamespace(), element.getParent().getXmlTag())
            )
          );
        }
      }
      else if (element instanceof GenericDomValue) {
        return ContainerUtil.createMaybeSingletonList(checkRequiredGenericValue((GenericDomValue)element, required, holder));
      }
    }
    if (DomUtil.hasXml(element)) {
      final SmartList<DomElementProblemDescriptor> list = new SmartList<DomElementProblemDescriptor>();
      final DomGenericInfo info = element.getGenericInfo();
      for (final AbstractDomChildrenDescription description : info.getChildrenDescriptions()) {
        if (description instanceof DomCollectionChildDescription && description.getValues(element).isEmpty()) {
          final DomCollectionChildDescription childDescription = (DomCollectionChildDescription)description;
          final Required annotation = description.getAnnotation(Required.class);
          if (annotation != null && annotation.value()) {
            list.add(holder.createProblem(element, childDescription, IdeBundle.message("child.tag.0.should.be.defined", ((DomCollectionChildDescription)description).getXmlElementName())));
          }
        }
      }
      return list;
    }
    return Collections.emptyList();
  }

  @NotNull
  public List<DomElementProblemDescriptor> checkResolveProblems(GenericDomValue element, final DomElementAnnotationHolder holder) {
    if (StringUtil.isEmpty(element.getStringValue())) {
      final Required required = element.getAnnotation(Required.class);
      if (required != null && !required.nonEmpty()) return Collections.emptyList();
    }

    final XmlElement valueElement = DomUtil.getValueElement(element);
    if (valueElement != null && !isSoftReference(element)) {
      final SmartList<DomElementProblemDescriptor> list = new SmartList<DomElementProblemDescriptor>();
      final PsiReference[] psiReferences = myProvider.getReferencesByElement(valueElement, new ProcessingContext());
      GenericDomValueReference domReference = null;
      for (final PsiReference reference : psiReferences) {
        if (reference instanceof GenericDomValueReference) {
          domReference = (GenericDomValueReference)reference;
          break;
        }
      }
      final Converter converter = WrappingConverter.getDeepestConverter(element.getConverter(), element);
      final boolean domReferenceResolveOK = domReference != null && !hasBadResolve(element, domReference)
        || domReference != null && converter instanceof ResolvingConverter && ((ResolvingConverter)converter).getAdditionalVariants(domReference.getConvertContext()).contains(element.getStringValue());
      boolean hasBadResolve = false;
      if (!domReferenceResolveOK) {
        for (final PsiReference reference : psiReferences) {
          if (reference != domReference && hasBadResolve(element, reference)) {
            hasBadResolve = true;
            list.add(holder.createResolveProblem(element, reference));
          }
        }
        final boolean isResolvingConverter = converter instanceof ResolvingConverter;
        if (!hasBadResolve &&
            (domReference != null || isResolvingConverter &&
                                     hasBadResolve(element, domReference = new GenericDomValueReference(element)))) {
          hasBadResolve = true;
          final String errorMessage = converter
            .getErrorMessage(element.getStringValue(), new ConvertContextImpl(DomManagerImpl.getDomInvocationHandler(element)));
          if (errorMessage != null && XmlHighlightVisitor.getErrorDescription(domReference) != null) {
            list.add(holder.createResolveProblem(element, domReference));
          }
        }
      }
      if (!hasBadResolve && psiReferences.length == 0 && element.getValue() == null && !PsiTreeUtil.hasErrorElements(valueElement)) {
        final String errorMessage = converter
          .getErrorMessage(element.getStringValue(), new ConvertContextImpl(DomManagerImpl.getDomInvocationHandler(element)));
        if (errorMessage != null) {
          list.add(holder.createProblem(element, errorMessage));
        }
      }
      return list;
    }
    return Collections.emptyList();
  }

  @NotNull
  public List<DomElementProblemDescriptor> checkNameIdentity(DomElement element, final DomElementAnnotationHolder holder) {
    final String elementName = ElementPresentationManager.getElementName(element);
    if (StringUtil.isNotEmpty(elementName)) {
      final DomElement domElement = DomUtil.findDuplicateNamedValue(element, elementName);
      if (domElement != null) {
        final String typeName = ElementPresentationManager.getTypeNameForObject(element);
        final GenericDomValue genericDomValue = domElement.getGenericInfo().getNameDomElement(element);
        if (genericDomValue != null) {
          return Arrays.asList(holder.createProblem(genericDomValue, DomUtil.getFile(domElement).equals(DomUtil.getFile(element))
                                                                     ? IdeBundle.message("model.highlighting.identity", typeName)
                                                                     : IdeBundle.message("model.highlighting.identity.in.other.file", typeName,
                                                                                         domElement.getXmlTag().getContainingFile().getName())));
        }
      }
    }
    return Collections.emptyList();
  }

  private static boolean hasBadResolve(GenericDomValue value, PsiReference reference) {
    return XmlHighlightVisitor.hasBadResolve(reference);
  }

  private static boolean isSoftReference(GenericDomValue value) {
    final Resolve resolve = value.getAnnotation(Resolve.class);
    if (resolve != null && resolve.soft()) return true;

    final Convert convert = value.getAnnotation(Convert.class);
    if (convert != null && convert.soft()) return true;

    final Referencing referencing = value.getAnnotation(Referencing.class);
    if (referencing != null && referencing.soft()) return true;

    return false;
  }

  @Nullable
  private static DomElementProblemDescriptor checkRequiredGenericValue(final GenericDomValue child, final Required required,
                                                                       final DomElementAnnotationHolder annotator) {
    final String stringValue = child.getStringValue();
    if (stringValue == null) return null;

    if (required.nonEmpty() && isEmpty(child, stringValue)) {
      return annotator.createProblem(child, IdeBundle.message("value.must.not.be.empty"));
    }
    if (required.identifier() && !isIdentifier(stringValue)) {
      return annotator.createProblem(child, IdeBundle.message("value.must.be.identifier"));
    }
    return null;
  }

  private static boolean isIdentifier(final String s) {
    if (StringUtil.isEmptyOrSpaces(s)) return false;

    if (!Character.isJavaIdentifierStart(s.charAt(0))) return false;

    for (int i = 1; i < s.length(); i++) {
      if (!Character.isJavaIdentifierPart(s.charAt(i))) return false;
    }

    return true;
  }

  private static boolean isEmpty(final GenericDomValue child, final String stringValue) {
    if (stringValue.trim().length() != 0) {
      return false;
    }
    if (child instanceof GenericAttributeValue) {
      final XmlAttributeValue value = ((GenericAttributeValue)child).getXmlAttributeValue();
      if (value != null && value.getTextRange().isEmpty()) {
        return false;
      }
    }
    return true;
  }


  private static class AddRequiredSubtagFix implements LocalQuickFix, IntentionAction {
    private final String tagName;
    private final String tagNamespace;
    private final XmlTag parentTag;

    public AddRequiredSubtagFix(@NotNull String _tagName, @NotNull String _tagNamespace, @NotNull XmlTag _parentTag) {
      tagName = _tagName;
      tagNamespace = _tagNamespace;
      parentTag = _parentTag;
    }

    @NotNull
    public String getName() {
      return XmlBundle.message("insert.required.tag.fix", tagName);
    }

    @NotNull
    public String getText() {
      return getName();
    }

    @NotNull
    public String getFamilyName() {
      return getName();
    }

    public boolean isAvailable(@NotNull final Project project, final Editor editor, final PsiFile file) {
      return true;
    }

    public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
      doFix();
    }

    public boolean startInWriteAction() {
      return true;
    }

    public void applyFix(@NotNull final Project project, @NotNull final ProblemDescriptor descriptor) {
      doFix();
    }

    private void doFix() {
      if (!CodeInsightUtilBase.prepareFileForWrite(parentTag.getContainingFile())) return;

      try {
        parentTag.add(parentTag.createChildTag(tagName, tagNamespace, "",false));
      }
      catch (IncorrectOperationException e) {
        throw new RuntimeException(e);
      }
    }
  }
}