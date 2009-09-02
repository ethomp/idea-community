package com.intellij.psi.impl.light;

import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import com.intellij.openapi.fileTypes.StdFileTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;

public class LightModifierList extends LightElement implements PsiModifierList{
  public LightModifierList(PsiManager manager){
    super(manager, StdFileTypes.JAVA.getLanguage());
  }

  public boolean hasModifierProperty(@NotNull String name){
    return false;
  }

  public boolean hasExplicitModifier(@NotNull String name) {
    return false;
  }

  public void setModifierProperty(@NotNull String name, boolean value) throws IncorrectOperationException{
    throw new IncorrectOperationException();
  }

  public void checkSetModifierProperty(@NotNull String name, boolean value) throws IncorrectOperationException{
    throw new IncorrectOperationException();
  }

  @NotNull
  public PsiAnnotation[] getAnnotations() {
    return PsiAnnotation.EMPTY_ARRAY;
  }

  @NotNull
  public PsiAnnotation[] getApplicableAnnotations() {
    return getAnnotations();
  }

  public PsiAnnotation findAnnotation(@NotNull String qualifiedName) {
    return null;
  }

  @NotNull
  public PsiAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
    throw new UnsupportedOperationException("Method addAnnotation is not yet implemented in " + getClass().getName());
  }

  public String getText(){
    return "";
  }

  public void accept(@NotNull PsiElementVisitor visitor){
    if (visitor instanceof JavaElementVisitor) {
      ((JavaElementVisitor)visitor).visitModifierList(this);
    }
    else {
      visitor.visitElement(this);
    }
  }

  public PsiElement copy(){
    return null;
  }

  public String toString(){
    return "PsiModifierList";
  }

}