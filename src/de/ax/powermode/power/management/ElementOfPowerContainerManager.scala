/*
 * Copyright 2015 Baptiste Mesta
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
package de.ax.powermode.power.management

import java.awt._
import javax.swing._

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.{EditorFactoryAdapter, EditorFactoryEvent}
import de.ax.powermode.PowerMode

import scala.collection.mutable

/**
  * @author Baptiste Mesta
  */
class ElementOfPowerContainerManager extends EditorFactoryAdapter {
  val sparkContainers = mutable.Map.empty[Editor, ElementOfPowerContainer]

  val sparkContainerUpdateThread = new Thread(new Runnable() {
    def run {
      while (true) {
        PowerMode.getInstance.reduced
        sparkContainers.values.foreach(_.updateElementsOfPower())
        try {
          Thread.sleep(1000/PowerMode.getInstance.frameRate)
        }
        catch {
          case ignored: InterruptedException => {
          }
        }
      }
    }
  })
  sparkContainerUpdateThread.start()

  override def editorCreated( event: EditorFactoryEvent) {
    val editor: Editor = event.getEditor
    sparkContainers.put(editor, new ElementOfPowerContainer(editor))
  }

  override def editorReleased( event: EditorFactoryEvent) {
    sparkContainers.remove(event.getEditor)
  }

  def update( editor: Editor) {

    if (PowerMode.getInstance.isEnabled) {
      PowerMode.getInstance.updated
      SwingUtilities.invokeLater(new Runnable() {
        def run {
          updateInUI(editor)
        }
      })
    }
  }

  private def updateInUI( editor: Editor) {
    val caretPosition = getCaretPosition(editor)
    sparkContainers.get(editor).foreach(_.update(caretPosition))
  }

  def getCaretPosition(editor: Editor): Point = {
    val p: Point = editor.visualPositionToXY(editor.getCaretModel.getVisualPosition)
    val location = editor.getScrollingModel.getVisibleArea.getLocation
    p.translate(-location.x, -location.y)
    p
  }

  def dispose {
    sparkContainerUpdateThread.interrupt()
    sparkContainers.clear
  }
}