/*
 * Created by IntelliJ IDEA.
 * User: valentin
 * Date: 29.01.2004
 * Time: 21:10:56
 */
package com.intellij.openapi.fileTypes.ex;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class FakeFileType implements FileTypeIdentifiableByVirtualFile {

  @NotNull
  public String getDefaultExtension() {
    return "fakeExtension";
  }

  public Icon getIcon() {
    return null;
  }

  public boolean isBinary() {
    return true;
  }

  public boolean isReadOnly() {
    return true;
  }

  public String getCharset(@NotNull VirtualFile file, final byte[] content) {
    return null;
  }
}