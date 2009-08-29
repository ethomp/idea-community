package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlighingSetting;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightLevelUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.editor.HectorComponentPanel;
import com.intellij.openapi.editor.HectorComponentPanelsProvider;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * User: anna
 * Date: Jun 27, 2005
 */
public class HectorComponent extends JPanel {
  private static final Logger LOG = Logger.getInstance("com.intellij.openapi.editor.impl.HectorComponent");

  private WeakReference<JBPopup> myHectorRef;
  private final ArrayList<HectorComponentPanel> myAdditionalPanels;
  private final Map<Language, JSlider> mySliders;
  private final PsiFile myFile;

  private final String myTitle = EditorBundle.message("hector.highlighting.level.title");

  public HectorComponent(PsiFile file) {
    super(new GridBagLayout());
    myFile = file;
    mySliders = new HashMap<Language, JSlider>();

    final Project project = myFile.getProject();
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final VirtualFile virtualFile = myFile.getContainingFile().getVirtualFile();
    LOG.assertTrue(virtualFile != null);
    final boolean notInLibrary =
      !fileIndex.isInLibrarySource(virtualFile) && !fileIndex.isInLibraryClasses(virtualFile) || fileIndex.isInContent(virtualFile);
    final FileViewProvider viewProvider = myFile.getViewProvider();
    final Set<Language> languages = viewProvider.getLanguages();
    for (Language language : languages) {
      final Hashtable<Integer, JLabel> sliderLabels = new Hashtable<Integer, JLabel>();
      sliderLabels.put(1, new JLabel(EditorBundle.message("hector.none.slider.label")));
      sliderLabels.put(2, new JLabel(EditorBundle.message("hector.syntax.slider.label")));
      if (notInLibrary) {
        sliderLabels.put(3, new JLabel(EditorBundle.message("hector.inspections.slider.label")));
      }

      final JSlider slider = new JSlider(SwingConstants.VERTICAL, 1, notInLibrary ? 3 : 2, 1);
      slider.setLabelTable(sliderLabels);
      final boolean value = true;
      UIUtil.setSliderIsFilled(slider, value);
      slider.setPaintLabels(true);
      slider.setSnapToTicks(true);
      slider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          int value = slider.getValue();
          for (Enumeration<Integer> enumeration = sliderLabels.keys(); enumeration.hasMoreElements();) {
            Integer key = enumeration.nextElement();
            sliderLabels.get(key).setForeground(key.intValue() <= value ? Color.black : new Color(100, 100, 100));
          }
        }
      });
      final PsiFile psiRoot = viewProvider.getPsi(language);
      slider.setValue(getValue(HighlightLevelUtil.shouldHighlight(psiRoot), HighlightLevelUtil.shouldInspect(psiRoot)));
      mySliders.put(language, slider);
    }

    final DaemonCodeAnalyzer analyzer = DaemonCodeAnalyzer.getInstance(myFile.getProject());

    GridBagConstraints gc = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                                                   GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);

    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(IdeBorderFactory.createTitledBorder(myTitle));
    final boolean addLabel = mySliders.size() > 1;
    if (addLabel) {
      layoutVertical(panel);
    }
    else {
      layoutHorizontal(panel);
    }
    gc.gridx = 0;
    gc.gridy = 2;
    gc.weighty = 1.0;
    gc.fill = GridBagConstraints.BOTH;
    add(panel, gc);

    gc.gridy = GridBagConstraints.RELATIVE;
    gc.weighty = 0;
    myAdditionalPanels = new ArrayList<HectorComponentPanel>();
    for (HectorComponentPanelsProvider provider : Extensions.getExtensions(HectorComponentPanelsProvider.EP_NAME, project)) {
      final HectorComponentPanel componentPanel = provider.createConfigurable(file);
      if (componentPanel != null) {
        myAdditionalPanels.add(componentPanel);
        add(componentPanel.createComponent(), gc);
        componentPanel.reset();
      }
    }
  }

  public Dimension getPreferredSize() {
    final Dimension preferredSize = super.getPreferredSize();
    final int width = getFontMetrics(getFont()).stringWidth(myTitle) + 60;
    if (preferredSize.width < width){
      preferredSize.width = width;
    }
    return preferredSize;
  }

  private void layoutHorizontal(final JPanel panel) {
    for (JSlider slider : mySliders.values()) {
      slider.setOrientation(SwingConstants.HORIZONTAL);
      slider.setPreferredSize(new Dimension(200, 40));
      panel.add(slider, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                                               new Insets(0, 0, 0, 0), 0, 0));
    }
  }

  private void layoutVertical(final JPanel panel) {
    for (Language language : mySliders.keySet()) {
      JSlider slider = mySliders.get(language);
      JPanel borderPanel = new JPanel(new BorderLayout());
      slider.setPreferredSize(new Dimension(80, 100));
      borderPanel.add(new JLabel(language.getID()), BorderLayout.NORTH);
      borderPanel.add(slider, BorderLayout.CENTER);
      panel.add(borderPanel, new GridBagConstraints(GridBagConstraints.RELATIVE, 1, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
                                                    new Insets(0, 0, 0, 0), 0, 0));
    }
  }

  public void showComponent(RelativePoint point) {
    final JBPopup hector = JBPopupFactory.getInstance().createComponentPopupBuilder(this, this)
      .setRequestFocus(true)
      .setMovable(true)
      .setCancelCallback(new Computable<Boolean>() {
        public Boolean compute() {
          for (HectorComponentPanel additionalPanel : myAdditionalPanels) {
            if (!additionalPanel.canClose()) {
              return Boolean.FALSE;
            }
          }
          onClose();
          return Boolean.TRUE;
        }
      })
      .createPopup();
    final JBPopup oldHector = getOldHector();
    if (oldHector != null){
      oldHector.cancel();
    } else {
      myHectorRef = new WeakReference<JBPopup>(hector);
      hector.show(point);
    }
  }

  private JBPopup getOldHector(){
    if (myHectorRef == null) return null;
    final JBPopup hector = myHectorRef.get();
    if (hector == null || !hector.isVisible()){
      myHectorRef = null;
      return null;
    }
    return hector;
  }

  private void onClose() {
    if (isModified()) {
      for (HectorComponentPanel panel : myAdditionalPanels) {
        try {
          panel.apply();
        }
        catch (ConfigurationException e) {
          //shouldn't be
        }
      }
      forceDaemonRestart();
      InspectionProjectProfileManager.getInstance(myFile.getProject()).updateStatusBar();
    }
  }

  private void forceDaemonRestart() {
    final FileViewProvider viewProvider = myFile.getViewProvider();
    for (Language language : mySliders.keySet()) {
      JSlider slider = mySliders.get(language);
      PsiElement root = viewProvider.getPsi(language);
      int value = slider.getValue();
      if (value == 1) {
        HighlightLevelUtil.forceRootHighlighting(root, FileHighlighingSetting.SKIP_HIGHLIGHTING);
      }
      else if (value == 2) {
        HighlightLevelUtil.forceRootHighlighting(root, FileHighlighingSetting.SKIP_INSPECTION);
      }
      else {
        HighlightLevelUtil.forceRootHighlighting(root, FileHighlighingSetting.FORCE_HIGHLIGHTING);
      }
    }
    final DaemonCodeAnalyzer analyzer = DaemonCodeAnalyzer.getInstance(myFile.getProject());
    analyzer.restart();
  }

  private boolean isModified() {
    final FileViewProvider viewProvider = myFile.getViewProvider();
    for (Language language : mySliders.keySet()) {
      JSlider slider = mySliders.get(language);
      final PsiFile root = viewProvider.getPsi(language);
      if (getValue(HighlightLevelUtil.shouldHighlight(root), HighlightLevelUtil.shouldInspect(root)) != slider.getValue()) {
        return true;
      }
    }
    for (HectorComponentPanel panel : myAdditionalPanels) {
      if (panel.isModified()) {
        return true;
      }
    }

    return false;
  }

  private static int getValue(boolean isSyntaxHighlightingEnabled, boolean isInspectionsHighlightingEnabled) {
    if (!isSyntaxHighlightingEnabled && !isInspectionsHighlightingEnabled) {
      return 1;
    }
    if (isSyntaxHighlightingEnabled && !isInspectionsHighlightingEnabled) {
      return 2;
    }
    return 3;
  }
}