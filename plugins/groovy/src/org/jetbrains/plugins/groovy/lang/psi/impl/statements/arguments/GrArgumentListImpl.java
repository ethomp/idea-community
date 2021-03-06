/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.groovy.lang.psi.impl.statements.arguments;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiElementImpl;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes.mCOMMA;

/**
 * @author ilyas
 */
public class GrArgumentListImpl extends GroovyPsiElementImpl implements GrArgumentList {

  public GrArgumentListImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(GroovyElementVisitor visitor) {
    visitor.visitArgumentList(this);
  }

  public String toString() {
    return "Arguments";
  }

  @NotNull
  public GrNamedArgument[] getNamedArguments() {
    return findChildrenByClass(GrNamedArgument.class);
  }

  @Override
  public GrNamedArgument findNamedArgument(@NotNull String label) {
    for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
      if (cur instanceof GrNamedArgument) {
        if (label.equals(((GrNamedArgument)cur).getLabelName())) {
          return (GrNamedArgument)cur;
        }
      }
    }

    return null;
  }

  @NotNull
  public GrExpression[] getExpressionArguments() {
    List<GrExpression> result = new ArrayList<GrExpression>();
    for (PsiElement cur = this.getFirstChild(); cur != null; cur = cur.getNextSibling()) {
      if (cur instanceof GrExpression) result.add((GrExpression)cur);
    }
    return result.toArray(new GrExpression[result.size()]);
  }

  @NotNull
  @Override
  public PsiElement[] getAllArguments() {
    List<PsiElement> args = new ArrayList<PsiElement>();
    for (PsiElement child = getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof GrNamedArgument || child instanceof GrExpression) args.add(child);
    }
    return args.toArray(new PsiElement[args.size()]);
  }

  public GrArgumentList replaceWithArgumentList(GrArgumentList newArgList) throws IncorrectOperationException {
    if (this.getParent() == null || this.getParent().getNode() == null) {
      throw new IncorrectOperationException();
    }
    ASTNode parentNode = this.getParent().getNode();
    ASTNode newNode = newArgList.getNode();
    assert parentNode != null;
    parentNode.replaceChild(this.getNode(), newNode);
    if (!(newNode.getPsi() instanceof GrArgumentList)) {
      throw new IncorrectOperationException();
    }
    return ((GrArgumentList)newNode.getPsi());
  }

  public boolean isIndexPropertiesList() {
    PsiElement firstChild = getFirstChild();
    if (firstChild == null) return false;
    ASTNode node = firstChild.getNode();
    assert node != null;
    return node.getElementType() == GroovyTokenTypes.mLBRACK;
  }

  @Nullable
  public PsiElement getLeftParen() {
    ASTNode paren = getNode().findChildByType(GroovyTokenTypes.mLPAREN);
    return paren != null ? paren.getPsi() : null;
  }

  @Nullable
  public PsiElement getRightParen() {
    ASTNode paren = getNode().findChildByType(GroovyTokenTypes.mRPAREN);
    return paren != null ? paren.getPsi() : null;
  }

  public int getExpressionArgumentIndex(final GrExpression arg) {
    return ArrayUtil.indexOf(getExpressionArguments(), arg);    
  }

  @Nullable
  public GrExpression removeArgument(final int argNumber) {
    GrExpression[] arguments = getExpressionArguments();
    if (argNumber < 0 || arguments.length <= argNumber) return null;

    GrExpression expression = arguments[argNumber];
    expression.delete();
    return expression;
  }

  public GrNamedArgument addNamedArgument(final GrNamedArgument namedArgument) {
    final GrNamedArgument[] namedArguments = getNamedArguments();
    final GrExpression[] args = getExpressionArguments();
    PsiElement anchor = null;
    final int namedCount = namedArguments.length;
    final int exprCount = args.length;
    if (namedCount > 0) {
      anchor = namedArguments[namedCount - 1];
    }
    else if (exprCount > 0) {
      anchor = args[exprCount - 1];
    }

    if (anchor != null) {
      anchor = PsiUtil.getNextNonSpace(anchor);
    }
    else {
      anchor = getRightParen();
    }

    addBefore(namedArgument, anchor);
    return namedArgument;
  }

  @Override
  public PsiElement addBefore(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    if (element instanceof GrNamedArgument || element instanceof GrExpression) {
      if (anchor == null) anchor = getLastChild();
      if (anchor == null) {
        return super.addBefore(element, anchor);
      }
      else {
        anchor = anchor.getPrevSibling();
      }
      while (anchor != null && !(anchor instanceof GrExpression) && !(anchor instanceof GrNamedArgument)) {
        anchor = anchor.getPrevSibling();
      }
      return addAfter(element, anchor);
    }
    return super.addBefore(element, anchor);
  }

  @Override
  public PsiElement addAfter(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    if (element instanceof GrExpression || element instanceof GrNamedArgument) {
      final boolean insertComma = getAllArguments().length != 0;

      if (anchor == null) anchor = getLeftParen();

      PsiElement result;
      result = super.addAfter(element, anchor);
      if (insertComma) {
        final ASTNode astNode = getNode();
        if (anchor == getLeftParen()) {
          astNode.addLeaf(mCOMMA, ",", result.getNextSibling().getNode());
        }
        else {
          astNode.addLeaf(mCOMMA, ",", result.getNode());
        }
        CodeStyleManager.getInstance(getManager().getProject()).reformat(this);
      }

      return result;
    }
    return super.addAfter(element, anchor);
  }

  public void deleteChildInternal(@NotNull ASTNode child) {
    PsiElement element = child.getPsi();
    if (element instanceof GrExpression || element instanceof GrNamedArgument) {
      ASTNode prev = TreeUtil.skipElementsBack(child.getTreePrev(), GroovyTokenTypes.WHITE_SPACES_OR_COMMENTS);
      if (prev != null && prev.getElementType() == mCOMMA) {
        super.deleteChildInternal(prev);
      }
      else {
        ASTNode next = TreeUtil.skipElements(child.getTreeNext(), GroovyTokenTypes.WHITE_SPACES_OR_COMMENTS);
        if (next != null && next.getElementType() == mCOMMA) {
          deleteChildInternal(next);
        }
      }
    }
    super.deleteChildInternal(child);
  }
}