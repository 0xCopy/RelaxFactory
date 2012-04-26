/*
 * Copyright 2011 Google Inc.
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
package oauth2.client;

/**
 * A callback for any asynchronous call that can result in success or failure.
 * 
 * @param <T> The type returned on success
 * @param <F> The type returned on failure
 * 
 * @deprecated This class is simply a stand-in for GWT's {@link Callback} class,
 * and the two can be used interchangeably within this library. It is included
 * purely for backwards compatibility. It will be removed soon.
 */
@Deprecated
public interface Callback<T, F>
    extends com.google.gwt.core.client.Callback<T, F> {
}
