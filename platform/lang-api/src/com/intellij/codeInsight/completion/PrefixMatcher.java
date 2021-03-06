/*
 * Copyright (c) 2008 Your Corporation. All Rights Reserved.
 */
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
public abstract class PrefixMatcher {
  public static final PrefixMatcher FALSE_MATCHER = new PrefixMatcher("######################################") {

    public boolean prefixMatches(@NotNull final String name) {
      return false;
    }

    @NotNull
    public PrefixMatcher cloneWithPrefix(@NotNull final String prefix) {
      return this;
    }
  };
  protected final String myPrefix;

  protected PrefixMatcher(String prefix) {
    myPrefix = prefix;
  }

  public boolean prefixMatches(@NotNull LookupElement element) {
    for (String s : element.getAllLookupStrings()) {
      if (prefixMatches(s)) {
        return true;
      }
    }
    return false;
  }

  public abstract boolean prefixMatches(@NotNull String name);

  @NotNull
  public final String getPrefix() {
    return myPrefix;
  }

  @NotNull public abstract PrefixMatcher cloneWithPrefix(@NotNull String prefix);
}
