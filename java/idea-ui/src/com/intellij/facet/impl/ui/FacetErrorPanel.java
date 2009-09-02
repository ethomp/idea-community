/*
 * Copyright (c) 2000-2007 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.facet.impl.ui;

import com.intellij.facet.ui.FacetConfigurationQuickFix;
import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.UserActivityListener;
import com.intellij.ui.UserActivityWatcher;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class FacetErrorPanel {
  @NonNls private static final String HTML_PREFIX = "<html><body>";
  @NonNls private static final String HTML_SUFFIX = "</body></html>";
  private final JPanel myMainPanel;
  private JPanel myButtonPanel;
  private JButton myQuickFixButton;
  private FacetConfigurationQuickFix myCurrentQuickFix;
  private final JLabel myWarningLabel;
  private final FacetValidatorsManagerImpl myValidatorsManager;
  private boolean myNoErrors = true;
  private final List<Runnable> myListeners = new ArrayList<Runnable>();

  public FacetErrorPanel() {
    myValidatorsManager = new FacetValidatorsManagerImpl();
    myWarningLabel = new JLabel();
    myWarningLabel.setIcon(Messages.getWarningIcon());
    myQuickFixButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (myCurrentQuickFix != null) {
          myCurrentQuickFix.run(myQuickFixButton);
        }
      }
    });
    myMainPanel = new JPanel(new BorderLayout());
    myMainPanel.add(BorderLayout.EAST, myButtonPanel);
    myMainPanel.add(BorderLayout.CENTER, myWarningLabel);
    setNoErrors();
  }

  public void addListener(Runnable listener) {
    myListeners.add(listener);
  }

  private void changeValidity(final boolean noErrors) {
    myNoErrors = noErrors;
    for (Runnable listener : myListeners) {
      listener.run();
    }
  }

  private void setNoErrors() {
    myMainPanel.setVisible(false);
    myWarningLabel.setVisible(false);
    myQuickFixButton.setVisible(false);
    changeValidity(true);
  }

  public void disposeUIResources() {
    myCurrentQuickFix = null;
  }

  public JComponent getComponent() {
    return myMainPanel;
  }

  public boolean isOk() {
    return myNoErrors;
  }

  public FacetValidatorsManager getValidatorsManager() {
    return myValidatorsManager;
  }

  private class FacetValidatorsManagerImpl implements FacetValidatorsManager {
    private final List<FacetEditorValidator> myValidators = new ArrayList<FacetEditorValidator>();

    public void registerValidator(final FacetEditorValidator validator, JComponent... componentsToWatch) {
      myValidators.add(validator);
      final UserActivityWatcher watcher = new UserActivityWatcher();
      for (JComponent component : componentsToWatch) {
        watcher.register(component);
      }
      watcher.addUserActivityListener(new UserActivityListener() {
        public void stateChanged() {
          validate();
        }
      });
    }

    public void validate() {
      for (FacetEditorValidator validator : myValidators) {
        ValidationResult validationResult = validator.check();
        if (!validationResult.isOk()) {
          myMainPanel.setVisible(true);
          myWarningLabel.setText(HTML_PREFIX + validationResult.getErrorMessage() + HTML_SUFFIX);
          myWarningLabel.setVisible(true);
          myCurrentQuickFix = validationResult.getQuickFix();
          myQuickFixButton.setVisible(myCurrentQuickFix != null);
          if (myCurrentQuickFix != null) {
            String buttonText = myCurrentQuickFix.getFixButtonText();
            myQuickFixButton.setText(buttonText != null ? buttonText : IdeBundle.message("button.facet.quickfix.text"));
          }
          changeValidity(false);
          return;
        }
      }
      myCurrentQuickFix = null;
      setNoErrors();
    }
  }
}