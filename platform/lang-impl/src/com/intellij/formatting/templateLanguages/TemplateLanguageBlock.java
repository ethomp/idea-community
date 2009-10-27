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
package com.intellij.formatting.templateLanguages;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexey Chmutov
 *         Date: Jun 26, 2009
 *         Time: 4:05:40 PM
 */
public abstract class TemplateLanguageBlock extends AbstractBlock implements BlockWithParent {
  private final TemplateLanguageBlockFactory myBlockFactory;
  private List<DataLanguageBlockWrapper> myForeignChildren;
  private boolean myChildrenBuilt = false;
  private BlockWithParent myParent;

  protected TemplateLanguageBlock(@NotNull TemplateLanguageBlockFactory blockFactory, @NotNull ASTNode node, @Nullable List<DataLanguageBlockWrapper> foreignChildren) {
    super(node, null, null);
    myBlockFactory = blockFactory;
    myForeignChildren = foreignChildren;
  }

  protected List<Block> buildChildren() {
    myChildrenBuilt = true;
    if (isLeaf()) {
      return EMPTY;
    }
    final ArrayList<TemplateLanguageBlock> tlChildren = new ArrayList<TemplateLanguageBlock>(5);
    for (ASTNode childNode = getNode().getFirstChildNode(); childNode != null; childNode = childNode.getTreeNext()) {
      if (FormatterUtil.containsWhiteSpacesOnly(childNode)) continue;
      if (childNode.getElementType() != getTemplateTextElementType() || noForeignChildren()) {
        final TemplateLanguageBlock childBlock = myBlockFactory.createTemplateLanguageBlock(childNode, null);
        childBlock.setParent(this);
        tlChildren.add(childBlock);
      }
    }
    final List<Block> children = (List<Block>)(myForeignChildren == null ? tlChildren : BlockUtil.mergeBlocks(tlChildren, myForeignChildren));
    //BlockUtil.printBlocks(getTextRange(), children);
    return BlockUtil.setParent(children, this);
  }

  private boolean noForeignChildren() {
    return (myForeignChildren == null || myForeignChildren.isEmpty());
  }

  void addForeignChild(@NotNull DataLanguageBlockWrapper foreignChild) {
    initForeignChildren();
    myForeignChildren.add(foreignChild);
  }

  void addForeignChildren(List<DataLanguageBlockWrapper> foreignChildren) {
    initForeignChildren();
    myForeignChildren.addAll(foreignChildren);
  }

  private void initForeignChildren() {
    assert !myChildrenBuilt;
    if (myForeignChildren == null) {
      myForeignChildren = new ArrayList<DataLanguageBlockWrapper>(5);
    }
  }

  @Nullable
  public Spacing getSpacing(Block child1, Block child2) {
    if (child1 instanceof DataLanguageBlockWrapper && child2 instanceof DataLanguageBlockWrapper) {
      return ((DataLanguageBlockWrapper)child1).getRightHandSpacing((DataLanguageBlockWrapper)child2);
    }
    return null;
  }

  public boolean isLeaf() {
    return noForeignChildren() && getNode().getFirstChildNode() == null;
  }

  protected abstract IElementType getTemplateTextElementType();

  public BlockWithParent getParent() {
    return myParent;
  }

  public void setParent(BlockWithParent newParent) {
    myParent = newParent;
  }
}
