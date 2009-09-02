/*
 * Copyright (c) 2000-2007 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.patterns;

import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
public class PsiExpressionPattern<T extends PsiExpression, Self extends PsiExpressionPattern<T,Self>> extends PsiJavaElementPattern<T,Self> {
  protected PsiExpressionPattern(final Class<T> aClass) {
    super(aClass);
  }

  public Self ofType(@NotNull final ElementPattern pattern) {
    return with(new PatternCondition<T>("ofType") {
      public boolean accepts(@NotNull final T t, final ProcessingContext context) {
        return pattern.getCondition().accepts(t.getType(), context);
      }
    });
  }

  public PsiMethodCallPattern methodCall(final ElementPattern<? extends PsiMethod> method) {
    return new PsiMethodCallPattern().and(this).with(new PatternCondition<PsiMethodCallExpression>("methodCall") {
      public boolean accepts(@NotNull PsiMethodCallExpression callExpression, ProcessingContext context) {
        final JavaResolveResult[] results = callExpression.getMethodExpression().multiResolve(true);
        for (JavaResolveResult result : results) {
          if (method.getCondition().accepts(result.getElement(), context)) {
            return true;
          }
        }
        return false;
      }
    });
  }

  public Self skipParentheses(final ElementPattern<? extends PsiExpression> expressionPattern) {
    return with(new PatternCondition<T>("skipParentheses") {
      @Override
      public boolean accepts(@NotNull T t, ProcessingContext context) {
        PsiExpression expression = t;
        while (expression instanceof PsiParenthesizedExpression) {
          expression = ((PsiParenthesizedExpression)expression).getExpression();
        }
        return expressionPattern.accepts(expression, context);
      }
    });
  }

  public static class Capture<T extends PsiExpression> extends PsiExpressionPattern<T, Capture<T>> {
    public Capture(final Class<T> aClass) {
      super(aClass);
    }

  }
}