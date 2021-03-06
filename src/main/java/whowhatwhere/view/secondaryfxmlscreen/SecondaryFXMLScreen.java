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
package whowhatwhere.view.secondaryfxmlscreen;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public abstract class SecondaryFXMLScreen
{
	private Stage postCloseStage;
	private Scene postCloseScene;
	private Parent loadedFXML;
	private FXMLLoader loader;
	private EventHandler<WindowEvent> eventForStageOnShowing;

	/**
	 * 
	 * @param fxmlLocation
	 *            - location of the fxml file relative to the resources dir
	 * @param stage
	 *            - the stage to come back to after this window is closed
	 * @param scene
	 *            - the scene to come back to after this window is closed
	 * @throws IOException
	 *             - if an error occurred while trying to load the fxml
	 */
	public SecondaryFXMLScreen(String fxmlLocation, Stage stage, Scene scene) throws IOException
	{
		this.postCloseStage = stage;
		this.postCloseScene = scene;

		loader = new FXMLLoader(getClass().getResource(fxmlLocation));
		setLoadedFXML(loader.load());
	}
	
	/**Allows setting an onShowing event for the stage
	 * @param eventHandler - The event handler to run when the stage is set to "showing"
	 */
	public void setStageOnShowing(EventHandler<WindowEvent> eventHandler)
	{
		eventForStageOnShowing = eventHandler;
	}

	/**
	 * Shows the screen on the current stage
	 * 
	 * @param existingStage
	 *            - The stage on which to show the new screen.
	 * 
	 * @param buttonsToClose
	 *            - list of buttons that close this window. The buttons'
	 *            onAction doesn't have to implement anything, not even closing
	 *            the screen. If onAction is implemented, it <b>MUST</b> be done
	 *            before calling this method, and then that implementation will
	 *            be called before closing the window. In case the window
	 *            shouldn't be closed (if there's an input error on a form for
	 *            example), the inheriting class' onAction method implementation
	 *            should throw an IllegalArgumentException, which will then not
	 *            close the window.
	 */
	public void showScreenOnExistingStage(Stage existingStage, Button... buttonsToClose)
	{
		Scene scene = new Scene(getLoadedFXML());

		for (Button btn : buttonsToClose)
		{
			EventHandler<ActionEvent> onAction = btn.getOnAction();

			btn.setOnAction(event ->
			{
				try
				{
					if (onAction != null) //if there was already a button handler, run it, and then close the screen
						onAction.handle(event);
				}
				catch (IllegalArgumentException iae) //some error occurred (like invalid data entry), don't close the window 
				{
					return;
				}

				postCloseStage.setScene(postCloseScene);
				postCloseStage.show();
			});
		}

		existingStage.setScene(scene);
		
		if (eventForStageOnShowing != null)
		{
			existingStage.setOnShowing(eventForStageOnShowing);
			existingStage.hide(); //to activate onShowing() in case it was set
		}
		
		existingStage.show();
	}

	/**
	 * Shows the screen on a new stage
	 * 
	 * @param title
	 *            - Title for the new stage
	 * @param modality
	 *            - The modality of the new stage. If null, default modality is
	 *            used ({@code Modality.NONE}).
	 * @param buttonsToClose
	 *            - list of buttons that close this window. The buttons'
	 *            onAction doesn't have to implement anything, not even closing
	 *            the screen. If onAction is implemented, it <b>MUST</b> be done
	 *            before calling this method, and then that implementation will
	 *            be called before closing the window. In case the window
	 *            shouldn't be closed (if there's an input error on a form for
	 *            example), the inheriting class' onAction method implementation
	 *            should throw an IllegalArgumentException, which will then not
	 *            close the window.
	 * @return The new stage that was created for this screen. Users can then
	 *         call setOnCloseRequest() on it to decide behavior on external
	 *         (non {@code buttonsToClose}) close request
	 */
	public Stage showScreenOnNewStage(String title, Modality modality, Button... buttonsToClose)
	{
		Scene scene = new Scene(getLoadedFXML());
		Stage stage = new Stage();

		stage.setTitle(title);
		stage.getIcons().addAll(postCloseStage.getIcons());

		for (Button btn : buttonsToClose)
		{
			EventHandler<ActionEvent> onAction = btn.getOnAction();

			btn.setOnAction(event ->
			{
				try
				{
					if (onAction != null) //if there was already a button handler, run it, and then close the screen
						onAction.handle(event);
				}
				catch (IllegalArgumentException iae) //some error occurred (like invalid data entry), don't close the window
				{
					return;
				}

				stage.close();
			});
		}
		stage.setScene(scene);

		if (modality != null)
			stage.initModality(modality);
		
		if (eventForStageOnShowing != null)
			stage.setOnShowing(eventForStageOnShowing);

		stage.show();
		fitToVisualBoundsIfTooBig(stage);

		return stage;
	}
	
	public static void fitToVisualBoundsIfTooBig(Stage stage)
	{
		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
		double visualBoundHeight = primaryScreenBounds.getHeight();
		double visualBoundWidth = primaryScreenBounds.getWidth();

		if (visualBoundHeight < stage.getHeight())
		{
			stage.setHeight(visualBoundHeight);
			stage.setY(0);
		}

		if (visualBoundWidth < stage.getWidth())
		{
			stage.setWidth(visualBoundWidth);
			stage.setX(0);
		}
	}

	public Scene getPostCloseScene()
	{
		return postCloseScene;
	}

	public void setPostCloseScene(Scene postCloseScene)
	{
		this.postCloseScene = postCloseScene;
	}

	public Stage getPostCloseStage()
	{
		return postCloseStage;
	}

	public void setPostCloseStage(Stage stage)
	{
		this.postCloseStage = stage;
	}

	public Parent getLoadedFXML()
	{
		return loadedFXML;
	}

	public void setLoadedFXML(Parent loadedFXML)
	{
		this.loadedFXML = loadedFXML;
	}

	public FXMLLoader getLoader()
	{
		return loader;
	}
}
