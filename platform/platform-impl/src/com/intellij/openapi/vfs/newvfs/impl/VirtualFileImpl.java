/*
 * @author max
 */
package com.intellij.openapi.vfs.newvfs.impl;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

public class VirtualFileImpl extends VirtualFileSystemEntry {
  public VirtualFileImpl(final String name, final VirtualDirectoryImpl parent, final int id) {
    super(name, parent, id);
  }

  @Nullable
  public NewVirtualFile findChild(@NotNull @NonNls final String name) {
    return null;
  }

  public Collection<VirtualFile> getCachedChildren() {
    return Collections.emptyList();
  }

  @NotNull
  public Collection<VirtualFile> getInDbChildren() {
    return Collections.emptyList();
  }

  @NotNull
  public NewVirtualFileSystem getFileSystem() {
    final VirtualFileSystemEntry parent = getParent();
    assert parent != null;

    return parent.getFileSystem();
  }

  @Nullable
  public NewVirtualFile refreshAndFindChild(final String name) {
    return null;
  }

  @Nullable
  public NewVirtualFile findChildIfCached(final String name) {
    return null;
  }

  public VirtualFile[] getChildren() {
    return EMPTY_ARRAY;
  }

  public boolean isDirectory() {
    return false;
  }

  @NotNull
  public InputStream getInputStream() throws IOException {
    return ourPersistence.getInputStream(this);
  }

  @NotNull
  public byte[] contentsToByteArray() throws IOException {
    return ourPersistence.contentsToByteArray(this);
  }

  @NotNull
  public OutputStream getOutputStream(final Object requestor, final long modStamp, final long timeStamp) throws IOException {
    return ourPersistence.getOutputStream(this, requestor, modStamp, timeStamp);
  }

  public NewVirtualFile findChildById(int id) {
    return null;
  }
}