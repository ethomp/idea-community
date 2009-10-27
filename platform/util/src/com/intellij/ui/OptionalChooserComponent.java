package com.intellij.ui;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author oleg
 * This component represents a list of checkboxes.
 */
public abstract class OptionalChooserComponent implements CheckBoxListListener {
  private JPanel myContentPane;
  private CheckBoxList myList;
  private DefaultListModel myListModel;
  private List<Pair<String, Boolean>> myInitialList;
  private ArrayList<Pair<String, Boolean>> myWorkingList;

  public OptionalChooserComponent(@NotNull final List<Pair<String, Boolean>> list) {
    myInitialList = list;
    myWorkingList = new ArrayList<Pair<String, Boolean>>(myInitialList);

    // fill list
    reset();
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public void checkBoxSelectionChanged(int index, boolean value) {
    final Pair<String, Boolean> pair = myWorkingList.remove(index);
    myWorkingList.add(index, Pair.create(pair.first, value));
  }

  private void createUIComponents() {
    myListModel = new DefaultListModel();
    myList = new CheckBoxList(myListModel, this);
  }

  public void reset() {
    myWorkingList = new ArrayList<Pair<String, Boolean>>(myInitialList);
    update();
  }

  public abstract JCheckBox createCheckBox(final String path, final boolean checked);

  public boolean isModified() {
    return !myWorkingList.equals(myInitialList);
  }

  public ArrayList<Pair<String, Boolean>> getValue() {
    return myWorkingList;
  }

  public void apply() {
    myInitialList.clear();
    myInitialList.addAll(myWorkingList);
  }

  public void update() {
    myListModel.clear();
    for (Pair<String, Boolean> pair : myWorkingList) {
      myListModel.addElement(createCheckBox(pair.first, pair.second));
    }
  }
}