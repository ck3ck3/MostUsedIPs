package mostusedips;

import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application
{
	private final static String releaseVersion = "0.01";
	private final static String urlForLatestRelease = "https://api.github.com/repos/ck3ck3/MostUsedIPs/releases/latest";
	private static final int connectionTimeout = 5000;
	private static final int readTimeout = 5000;
	
	private static final String website = "http://ck3ck3.github.io/MostUsedIPs/";

	private final static String iconResource16 = "/ip16.jpg";
	private final static String iconResource32 = "/ip32.jpg";
	private final static String DLLx86Location = "/native/windows/x86/jnetpcap.dll";
	private final static String DLLx64Location = "/native/windows/x86_64/jnetpcap.dll";
	private final static String DLLName = "jnetpcap";

	private final static String appTitle = "Most Used IPs";
	private final static String mainFormLocation = "/mostusedips/view/MainForm.fxml";
	private final static int windowSizeX = 1024;
	private final static int windowSizeY = 790;

	private Logger logger;

	@Override
	public void start(Stage primaryStage)
	{
		try
		{
			initLogger();

			if (!loadJnetpcapDll())
			{
				System.err.println("Unable to load jnetpcap native dll. See log file for details. Unable to continue, aborting.");
				return;
			}

			if (!initSysTray(primaryStage))
				logger.log(Level.WARNING, "Unable to initialize system tray");

			URL fxmlLocation = Main.class.getResource(mainFormLocation);
			Parent root = FXMLLoader.load(fxmlLocation);

			Scene scene = new Scene(root, windowSizeX, windowSizeY);
			primaryStage.setTitle(appTitle);
			primaryStage.setScene(scene);
			primaryStage.show();
		}
		catch (IOException e)
		{
			logger.log(Level.SEVERE, "Unable to open file", e);
		}
	}

	/**
	 * @return true if successfully loaded, false otherwise
	 */
	private boolean loadJnetpcapDll()
	{
		try
		{
			System.loadLibrary(DLLName); //expected to throw exception on first run only
		}
		catch (UnsatisfiedLinkError ule)
		{
			try
			{
				if (!tryLoadingDll(DLLx86Location, DLLName, false)) //if loading the x86 version failed, no need to log the error yet. try loading the x64 version
					return tryLoadingDll(DLLx64Location, DLLName, true); //if this fails, write a log entry
			}
			catch (IOException ioe)
			{
				logger.log(Level.SEVERE, "Unable to copy dll from resources", ioe);
				return false;
			}
		}

		return true;
	}

	/**
	 * @param copyDllFrom
	 *            - relative path from the resources dir to the dll to be copied
	 * @param libName
	 *            - the library's name (without the ".dll")
	 * @param logULE
	 *            - if true, caught UnsatisfiedLinkError will be logged.
	 * @return true if successfully loaded, false otherwise
	 */
	private boolean tryLoadingDll(String copyDllFrom, String libName, boolean logULE) throws IOException
	{
		try
		{
			String currDir = System.getProperty("user.dir");
			InputStream dll = Main.class.getResourceAsStream(copyDllFrom);

			if (dll == null)
				throw new IOException("Unable to find " + copyDllFrom + " in resources");

			FileOutputStream dstFile = new FileOutputStream(currDir + "/" + libName + ".dll");

			IOUtils.copy(dll, dstFile);
			IOUtils.closeQuietly(dstFile);

			System.loadLibrary(libName);
		}
		catch (UnsatisfiedLinkError ule)
		{
			if (logULE)
				logger.log(Level.SEVERE, "Unable to load " + copyDllFrom, ule);

			return false;
		}

		return true;
	}

	private void initLogger() throws IOException
	{
		logger = Logger.getLogger(getAppName());
		FileHandler fh = new FileHandler(getAppName() + ".log");
		fh.setFormatter(new SimpleFormatter());
		logger.addHandler(fh);
	}

	private boolean initSysTray(Stage primaryStage)
	{
		TrayIcon trayIcon;
		SystemTray tray;

		if (SystemTray.isSupported())
		{
			primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream(iconResource16)));
			primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream(iconResource32)));
			
			tray = SystemTray.getSystemTray();
			java.awt.Image image = Toolkit.getDefaultToolkit().getImage(Main.class.getResource(iconResource16));

			Platform.setImplicitExit(false); //needed to keep the app running while minimized to tray

			trayIcon = new TrayIcon(image, appTitle);

			ActionListener listenerTray = new ActionListener()
			{
				@Override
				public void actionPerformed(java.awt.event.ActionEvent arg0)
				{
					Platform.runLater(new Runnable()
					{

						@Override
						public void run()
						{
							primaryStage.show();
							tray.remove(trayIcon);
						}
					});
				}
			};

			trayIcon.addActionListener(listenerTray);

			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>()
			{
				public void handle(WindowEvent we)
				{
					we.consume(); //ignore the application's exit button, instead minimize to systray
					Platform.runLater(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								tray.add(trayIcon);
								trayIcon.displayMessage("Minimized to tray", "Still running in the background, double click this icon to restore the window. Use the \"Exit\" button to exit.",
										MessageType.INFO);
								primaryStage.hide();
							}
							catch (Exception e)
							{
								logger.log(Level.WARNING, "Unable to minimize to tray", e);
							}
						}
					});
				}
			});

			return true;
		}
		else
			return false;
	}

	public static boolean isUpdateAvailable() throws IOException
	{
		URLConnection serviceURL;
		BufferedReader streamReader;
		String inputStr;
		StringBuilder responseStrBuilder = new StringBuilder();

		serviceURL = new URL(urlForLatestRelease).openConnection();
		serviceURL.setConnectTimeout(connectionTimeout);
		serviceURL.setReadTimeout(readTimeout);
		streamReader = new BufferedReader(new InputStreamReader(serviceURL.getInputStream()));

		while ((inputStr = streamReader.readLine()) != null)
			responseStrBuilder.append(inputStr);

		JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
		String version = (String) jsonObject.get("tag_name");

		return !version.equals(getReleaseVersion());
	}

	public static void main(String[] args)
	{
		launch(args);
	}
	
	public static String getAppName()
	{
		return appTitle;
	}
	
	public static String getReleaseVersion()
	{
		return releaseVersion;
	}
	
	public static String getWebsite()
	{
		return website;
	}
}
