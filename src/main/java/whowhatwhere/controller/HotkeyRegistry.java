/*******************************************************************************
 * Who What Where
 * Copyright (C) 2017  ck3ck3
 * https://github.com/ck3ck3/WhoWhatWhere
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package whowhatwhere.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import whowhatwhere.model.hotkey.HotkeyExecuter;
import whowhatwhere.model.hotkey.HotkeyManager;

public class HotkeyRegistry
{
	private Alert alertChangeHotkey;
	private HotkeyManager hotkeyManager = new HotkeyManager();
	private Node nodeToDisableOnKeyConfig;
	
	public HotkeyRegistry()
	{
		this(null);
	}
	
	public HotkeyRegistry(Node nodeToDisable)
	{
		this.nodeToDisableOnKeyConfig = nodeToDisable;
		
		initHotkeyChangeAlert();
	}
	
	public EventHandler<ActionEvent> generateEventHandlerForHotkeyConfigButton(String hotkeyID)
	{
		return new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent e)
			{
				hotkeyManager.setKeySelection(hotkeyID, true);
				
				if (nodeToDisableOnKeyConfig != null)
					nodeToDisableOnKeyConfig.setDisable(true);
				
				alertChangeHotkey.showAndWait();
			}
		};
	}
	
	public ChangeListener<Boolean> generateChangeListenerForHotkeyCheckbox(String hotkeyID, int defaultModifiers, int defaultKeycode, CheckBox chkbox, Label hotkeyLabel, Pane hotkeyPane,
			Runnable runnableKeyPressed)
	{
		return new ChangeListener<Boolean>()
		{
			boolean ignoreUncheck = false; //when we want to programmatically disable the box, without removing the key

			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val)
			{
				hotkeyPane.setDisable(!new_val);

				if (new_val)
				{
					try
					{
						int modifiers, keycode;

						try
						{
							modifiers = hotkeyManager.getHotkeyModifiers(hotkeyID);
							keycode = hotkeyManager.getHotkeyKeycode(hotkeyID);
						}
						catch (IllegalArgumentException iae) //first time we use this key
						{
							addHotkey(hotkeyID, defaultModifiers, defaultKeycode, hotkeyLabel, runnableKeyPressed);
							return;
						}

						addHotkey(hotkeyID, modifiers, keycode, hotkeyLabel, runnableKeyPressed);
					}
					catch (IllegalArgumentException iae)
					{
						new Alert(AlertType.ERROR, "This key combination is already in use. Change the hotkey that uses this combination, then try enabling this hotkey again.").showAndWait();
						hotkeyPane.setDisable(true);

						ignoreUncheck = true; //setSelected(false) will trigger this change listener, we manually ignore it just once
						chkbox.setSelected(false);
					}
				}
				else
				{
					if (!ignoreUncheck)
						hotkeyManager.removeHotkey(hotkeyID);

					ignoreUncheck = false;
				}
			}
		};
	}

	public void addHotkey(String hotkeyID, int modifiers, int keycode, Label hotkeyLabel, Runnable runnableKeyPressed)
	{
		HotkeyExecuter executer = generateHotkeyExecuter(hotkeyID, hotkeyLabel, runnableKeyPressed);

		hotkeyManager.addHotkey(hotkeyID, executer, modifiers, keycode);
		hotkeyLabel.setText("Current hotkey: " + HotkeyManager.hotkeyToString(modifiers, keycode));
	}

	private HotkeyExecuter generateHotkeyExecuter(String hotkeyID, Label hotkeyLabel, Runnable keyPressedRunnable)
	{
		return new HotkeyExecuter()
		{
			public void keyPressed(int modifiers, int keyCode, boolean isNewKey)
			{
				if (!isNewKey) //hotkey pressed
					Platform.runLater(keyPressedRunnable);
				else //new hotkey selection
				{
					try
					{
						hotkeyManager.modifyHotkey(hotkeyID, modifiers, keyCode);
					}
					catch (IllegalArgumentException iae) //failed to change hotkey
					{
						Platform.runLater(() ->
						{
							Alert error = new Alert(AlertType.ERROR);
							error.setTitle("Unable to change hotkey");
							error.setHeaderText("Failed to set a new hotkey");
							error.setContentText(iae.getMessage());

							alertChangeHotkey.close();
							error.showAndWait();
							if (nodeToDisableOnKeyConfig != null)
								nodeToDisableOnKeyConfig.setDisable(false);
						});

						return;
					}

					Platform.runLater(() ->
					{
						hotkeyLabel.setText("Current hotkey: " + HotkeyManager.hotkeyToString(modifiers, keyCode));
						alertChangeHotkey.close();
						if (nodeToDisableOnKeyConfig != null)
							nodeToDisableOnKeyConfig.setDisable(false);
					});
				}
			}
		};
	}

	private void initHotkeyChangeAlert()
	{
		alertChangeHotkey = new Alert(AlertType.NONE);

		alertChangeHotkey.setTitle("Change Hotkey");
		alertChangeHotkey.setHeaderText("Choose a new hotkey");
		alertChangeHotkey.setContentText("Press the new hotkey");
		alertChangeHotkey.getButtonTypes().add(new ButtonType("Cancel"));
		
		alertChangeHotkey.setOnCloseRequest(dialogEvent -> 
		{
			if (hotkeyManager.isKeySelection()) //if the dialog is getting closed without setting a new hotkey, disable the new key selection
			{
				hotkeyManager.setKeySelection(null, false);
				if (nodeToDisableOnKeyConfig != null)
					nodeToDisableOnKeyConfig.setDisable(false);
			}
		});
	}

	public void cleanup()
	{
		hotkeyManager.cleanup();		
	}

	public int getHotkeyKeycode(String hotkeyID)
	{
		return hotkeyManager.getHotkeyKeycode(hotkeyID);
	}

	public int getHotkeyModifiers(String hotkeyID)
	{
		return hotkeyManager.getHotkeyModifiers(hotkeyID);
	}
}
