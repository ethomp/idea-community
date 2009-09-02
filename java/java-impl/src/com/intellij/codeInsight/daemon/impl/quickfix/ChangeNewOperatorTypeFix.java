package com.intellij.codeInsight.daemon.impl.quickfix;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChangeNewOperatorTypeFix implements IntentionAction {
  private final PsiType myType;
  private final PsiNewExpression myExpression;

  private ChangeNewOperatorTypeFix(PsiType type, PsiNewExpression expression) {
    myType = type;
    myExpression = expression;
  }

  @NotNull
  public String getText() {
    return QuickFixBundle.message("change.new.operator.type.text", myExpression.getText(), myType.getPresentableText(), myType instanceof PsiArrayType ? "" : "()");
  }

  @NotNull
  public String getFamilyName() {
    return QuickFixBundle.message("change.new.operator.type.family");
  }

  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return myType.isValid()
           && myExpression.isValid()
           && myExpression.getManager().isInProject(myExpression)
           && !TypeConversionUtil.isPrimitiveAndNotNull(myType)
           && (myType instanceof PsiArrayType || myExpression.getArgumentList() != null)
      ;
  }

  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    if (!CodeInsightUtilBase.prepareFileForWrite(file)) return;
    changeNewOperatorType(myExpression, myType, editor);
  }

  private static void changeNewOperatorType(PsiNewExpression originalExpression, PsiType toType, final Editor editor) throws IncorrectOperationException {
    PsiNewExpression newExpression;
    PsiElementFactory factory = JavaPsiFacade.getInstance(originalExpression.getProject()).getElementFactory();
    int caretOffset;
    TextRange selection;
    if (toType instanceof PsiArrayType) {
      caretOffset = -2;
      @NonNls String text = "new " + toType.getDeepComponentType().getCanonicalText() + "[0]";
      for (int i = 1; i < toType.getArrayDimensions(); i++) {
        text += "[]";
        caretOffset -= 2;
      }

      newExpression = (PsiNewExpression)factory.createExpressionFromText(text, originalExpression);
      selection = new TextRange(caretOffset, caretOffset+1);
    }
    else {
      newExpression = (PsiNewExpression)factory.createExpressionFromText("new " + toType.getCanonicalText() + "()", originalExpression);
      PsiExpressionList argumentList = originalExpression.getArgumentList();
      if (argumentList == null) return;
      newExpression.getArgumentList().replace(argumentList);
      selection = null;
      caretOffset = -1;
    }
    PsiElement element = originalExpression.replace(newExpression);
    editor.getCaretModel().moveToOffset(element.getTextRange().getEndOffset() + caretOffset);
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    if (selection != null) {
      selection = selection.shiftRight(element.getTextRange().getEndOffset());
      editor.getSelectionModel().setSelection(selection.getStartOffset(), selection.getEndOffset());
    }
  }

  public boolean startInWriteAction() {
    return true;
  }

  public static void register(final HighlightInfo highlightInfo, PsiExpression expression, final PsiType lType) {
    expression = PsiUtil.deparenthesizeExpression(expression);
    if (!(expression instanceof PsiNewExpression)) return;
    final PsiType rType = expression.getType();
    PsiType newType = lType;
    if (rType instanceof PsiClassType && newType instanceof PsiClassType) {
      final PsiClassType.ClassResolveResult rResolveResult = ((PsiClassType)rType).resolveGenerics();
      final PsiClass rClass = rResolveResult.getElement();
      if (rClass != null) {
        final PsiClassType.ClassResolveResult lResolveResult = ((PsiClassType)newType).resolveGenerics();
        final PsiClass lClass = lResolveResult.getElement();
        if (lClass != null) {
          PsiSubstitutor substitutor = getInheritorSubstitutorForNewExpression(lClass, rClass, lResolveResult.getSubstitutor(), expression);
          if (substitutor != null) {
            newType = JavaPsiFacade.getInstance(lClass.getProject()).getElementFactory().createType(rClass, substitutor);
          }
        }
      }
    }
    PsiNewExpression newExpression = (PsiNewExpression)expression;
    QuickFixAction.registerQuickFixAction(highlightInfo, new ChangeNewOperatorTypeFix(newType, newExpression));
  }

  /* Guesswork
  */
  @Nullable
  private static PsiSubstitutor getInheritorSubstitutorForNewExpression(final PsiClass baseClass, final PsiClass inheritor,
                                                                       final PsiSubstitutor baseSubstitutor, final PsiElement context) {
    final Project project = baseClass.getProject();
    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    final PsiResolveHelper resolveHelper = facade.getResolveHelper();
    PsiSubstitutor superSubstitutor = TypeConversionUtil.getClassSubstitutor(baseClass, inheritor, PsiSubstitutor.EMPTY);
    if (superSubstitutor == null) return null;
    PsiSubstitutor inheritorSubstitutor = PsiSubstitutor.EMPTY;
    for (PsiTypeParameter inheritorParameter : PsiUtil.typeParametersIterable(inheritor)) {
      for (PsiTypeParameter baseParameter : PsiUtil.typeParametersIterable(baseClass)) {
        final PsiType substituted = superSubstitutor.substitute(baseParameter);
        PsiType arg = baseSubstitutor.substitute(baseParameter);
        if (arg instanceof PsiWildcardType) arg = ((PsiWildcardType)arg).getBound();
        PsiType substitution =
          resolveHelper.getSubstitutionForTypeParameter(inheritorParameter, substituted, arg, true, PsiUtil.getLanguageLevel(context));
        if (PsiType.NULL.equals(substitution)) continue;
        if (substitution == null) {
          return facade.getElementFactory().createRawSubstitutor(inheritor);
        }
        inheritorSubstitutor = inheritorSubstitutor.put(inheritorParameter, substitution);
        break;
      }
    }

    return inheritorSubstitutor;
  }
}