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

package com.intellij.openapi.components;

import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NonNls;

public class ComponentManagerConfig {
  @Tag(APPLICATION_COMPONENTS)
  @AbstractCollection(surroundWithTag = false)
  public ComponentConfig[] applicationComponents;

  @Tag(PROJECT_COMPONENTS)
  @AbstractCollection(surroundWithTag = false)
  public ComponentConfig[] projectComponents;

  @Tag(MODULE_COMPONENTS)
  @AbstractCollection(surroundWithTag = false)
  public ComponentConfig[] moduleComponents;

  @NonNls public static final String APPLICATION_COMPONENTS = "application-components";
  @NonNls public static final String PROJECT_COMPONENTS = "project-components";
  @NonNls public static final String MODULE_COMPONENTS = "module-components";
}
