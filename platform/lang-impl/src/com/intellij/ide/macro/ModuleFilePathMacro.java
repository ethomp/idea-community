package com.intellij.ide.macro;

import com.intellij.ide.DataAccessors;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.DataContext;

public final class ModuleFilePathMacro extends Macro {
  public String getName() {
    return "ModuleFilePath";
  }

  public String getDescription() {
    return IdeBundle.message("macro.module.file.path");
  }

  public String expand(DataContext dataContext) {
    return DataAccessors.MODULE_FILE_PATH.from(dataContext);
  }
}