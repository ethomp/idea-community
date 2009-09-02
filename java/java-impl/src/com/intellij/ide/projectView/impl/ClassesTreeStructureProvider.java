package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.SelectableTreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class ClassesTreeStructureProvider implements SelectableTreeStructureProvider, DumbAware {
  private final Project myProject;

  public ClassesTreeStructureProvider(Project project) {
    myProject = project;
  }

  public Collection<AbstractTreeNode> modify(AbstractTreeNode parent, Collection<AbstractTreeNode> children, ViewSettings settings) {
    final ProjectFileIndex index = ProjectRootManager.getInstance(myProject).getFileIndex();
    ArrayList<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
    for (final AbstractTreeNode child : children) {
      Object o = child.getValue();
      if (o instanceof PsiClassOwner) {
        final ViewSettings settings1 = ((ProjectViewNode)parent).getSettings();
        final PsiClassOwner classOwner = (PsiClassOwner)o;
        PsiClass[] classes = classOwner.getClasses();
        final VirtualFile file = classOwner.getVirtualFile();
        if (classes.length == 1 && !(classes[0] instanceof SyntheticElement) && file != null &&
            (index.isInSourceContent(file) || index.isInLibraryClasses(file) || index.isInLibrarySource(file))) {
          result.add(new ClassTreeNode(myProject, classes[0], settings1));
        } else {
          result.add(new PsiClassOwnerTreeNode(classOwner, settings1));
        }
        continue;
      }
      result.add(child);
    }
    return result;
  }

  public Object getData(Collection<AbstractTreeNode> selected, String dataName) {
    return null;
  }

  public PsiElement getTopLevelElement(final PsiElement element) {
    PsiFile baseRootFile = getBaseRootFile(element);
    if (baseRootFile == null) return null;
    PsiElement current = element;
    while (current != null) {
      if (current instanceof PsiFileSystemItem) {
        break;
      }
      if (isTopLevelClass(current, baseRootFile)) {
        break;
      }
      current = current.getParent();
    }

    if (current instanceof PsiClassOwner) {
      PsiClass[] classes = ((PsiClassOwner)current).getClasses();
      if (classes.length == 1 && !(classes[0] instanceof SyntheticElement) && isTopLevelClass(classes[0], baseRootFile)) {
        current = classes[0];
      }
    }
    return current instanceof PsiClass ? current : baseRootFile;
  }

  @Nullable
  private static PsiFile getBaseRootFile(PsiElement element) {
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) return null;

    final FileViewProvider viewProvider = containingFile.getViewProvider();
    return viewProvider.getPsi(viewProvider.getBaseLanguage());
  }

  private static boolean isTopLevelClass(final PsiElement element, PsiFile baseRootFile) {

    if (!(element instanceof PsiClass)) {
      return false;
    }
    final PsiElement parent = element.getParent();
                                        // do not select JspClass
    return parent instanceof PsiFile && parent.getLanguage() == baseRootFile.getLanguage();
  }

  private static class PsiClassOwnerTreeNode extends PsiFileNode {

    public PsiClassOwnerTreeNode(PsiClassOwner classOwner, ViewSettings settings) {
      super(classOwner.getProject(), classOwner, settings);
    }

    @Override
    public Collection<AbstractTreeNode> getChildrenImpl() {
      final ViewSettings settings = getSettings();
      final ArrayList<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
      for (PsiClass aClass : ((PsiClassOwner)getValue()).getClasses()) {
        if (!(aClass instanceof SyntheticElement)) {
          result.add(new ClassTreeNode(myProject, aClass, settings));
        }
      }
      return result;
    }
    
    protected void updateImpl(PresentationData data) {
      super.updateImpl(data);
      data.setPresentableText(getValue().getName());
      data.setIcons(getValue().getViewProvider().getVirtualFile().getIcon());
    }

  }
}