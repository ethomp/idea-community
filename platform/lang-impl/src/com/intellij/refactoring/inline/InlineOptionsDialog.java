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

package com.intellij.refactoring.inline;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.RadioUpDownListener;
import com.intellij.ui.IdeBorderFactory;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public abstract class InlineOptionsDialog extends RefactoringDialog implements InlineOptions {
  protected JRadioButton myRbInlineAll;
  protected JRadioButton myRbInlineThisOnly;
  protected boolean myInvokedOnReference;
  protected final PsiElement myElement;
  private final JLabel myNameLabel = new JLabel();
  protected JPanel myOptionsPanel;

  protected InlineOptionsDialog(Project project, boolean canBeParent, PsiElement element) {
    super(project, canBeParent);
    myElement = element;
  }

  protected JComponent createNorthPanel() {
    myNameLabel.setText(getNameLabelText());
    return myNameLabel;
  }

  public boolean isInlineThisOnly() {
    return myRbInlineThisOnly.isSelected();
  }

  protected JComponent createCenterPanel() {
    myOptionsPanel = new JPanel();
    myOptionsPanel.setBorder(IdeBorderFactory.createTitledBorder(getBorderTitle()));
    myOptionsPanel.setLayout(new BoxLayout(myOptionsPanel, BoxLayout.Y_AXIS));

    myRbInlineAll = new JRadioButton();
    myRbInlineAll.setText(getInlineAllText());
    myRbInlineAll.setSelected(true);
    myRbInlineThisOnly = new JRadioButton();
    myRbInlineThisOnly.setText(getInlineThisText());

    myOptionsPanel.add(myRbInlineAll);
    myOptionsPanel.add(myRbInlineThisOnly);
    ButtonGroup bg = new ButtonGroup();
    bg.add(myRbInlineAll);
    bg.add(myRbInlineThisOnly);
    new RadioUpDownListener(myRbInlineAll, myRbInlineThisOnly);

    myRbInlineThisOnly.setEnabled(myInvokedOnReference);
    final boolean writable = myElement.isWritable();
    myRbInlineAll.setEnabled(writable);
    if(myInvokedOnReference) {
      if (canInlineThisOnly()) {
        myRbInlineAll.setSelected(false);
        myRbInlineAll.setEnabled(false);
        myRbInlineThisOnly.setSelected(true);
      } else {
        if (writable) {
          final boolean inlineThis = isInlineThis();
          myRbInlineThisOnly.setSelected(inlineThis);
          myRbInlineAll.setSelected(!inlineThis);
        }
        else {
          myRbInlineAll.setSelected(false);
          myRbInlineThisOnly.setSelected(true);
        }
      }
    }
    else {
      myRbInlineAll.setSelected(true);
      myRbInlineThisOnly.setSelected(false);
    }

    getPreviewAction().setEnabled(myRbInlineAll.isSelected());
    myRbInlineAll.addItemListener(
      new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          boolean enabled = myRbInlineAll.isSelected();
          getPreviewAction().setEnabled(enabled);
        }
      }
    );
    return myOptionsPanel;
  }

  protected abstract String getNameLabelText();
  protected abstract String getBorderTitle();
  protected abstract String getInlineAllText();
  protected abstract String getInlineThisText();
  protected abstract boolean isInlineThis();
  protected boolean canInlineThisOnly() {
    return false;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myRbInlineThisOnly.isSelected() ? myRbInlineThisOnly : myRbInlineAll;
  }
}
