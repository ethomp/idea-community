/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package com.intellij.util.config;

import org.jetbrains.annotations.NonNls;

/**
 * @author dyoma
 */
public class ValueProperty<T> extends AbstractProperty<T> {
  private final T myDefault;
  private final String myName;

  public ValueProperty(@NonNls String name, T defaultValue) {
    myName = name;
    myDefault = defaultValue;
  }

  public T copy(T value) {
    return value;
  }

  public T getDefault(AbstractProperty.AbstractPropertyContainer container) {
    return myDefault;
  }

  public String getName() {
    return myName;
  }
}