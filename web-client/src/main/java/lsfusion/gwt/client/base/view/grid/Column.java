/*
 * Copyright 2010 Google Inc.
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
package lsfusion.gwt.client.base.view.grid;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.cell.Context;

public abstract class Column<T, C> {

  public Column() {
  }

  public abstract C getValue(T object);

  public abstract boolean isFocusable();

  public abstract void onEditEvent(EventHandler handler, boolean isBinding, Context editContext, Element editCellParent);

  public abstract void renderDom(Context context, Element cellElement, C value);

  public abstract void updateDom(Context context, Element cellElement, C value);
}
