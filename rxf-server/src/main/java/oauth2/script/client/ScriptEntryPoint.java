/*
 * Copyright (c) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package oauth2.script.client;


import com.google.gwt.core.client.EntryPoint;
import oauth2.client.Auth;

/**
 * An EntryPoint class that exports the {@link Auth#login()} method.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ScriptEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    Auth.export();
  }
}
