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
package com.intellij.cvsSupport2.connections.login;

import com.intellij.openapi.vcs.CalledInAwt;
import com.intellij.openapi.vcs.CalledInBackground;
import com.intellij.util.ThreeState;

// todo rename?
public interface CvsLoginWorker {
  /**
   * @return <code>true</code> if login attempt should be repeated after prompting user
   */
  @CalledInAwt
  boolean promptForPassword();

  @CalledInBackground
  ThreeState silentLogin(boolean forceCheck);

  void goOffline();
}