package whowhatwhere.controller;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**Gist taken from https://gist.github.com/darmbrust/9559744d1b1dada434a3 , modified to return void and ignore exceptions.<br>
 * {@link ToolTipDefaultsFixer}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class ToolTipDefaultsFixer
{
	/**
	 * Returns true if successful. Current defaults are 1000, 5000, 200;
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setTooltipTimers(long openDelay, long visibleDuration, long closeDelay)
	{
		try
		{
			Field f = Tooltip.class.getDeclaredField("BEHAVIOR");
			f.setAccessible(true);

			Class[] classes = Tooltip.class.getDeclaredClasses();
			for (Class clazz : classes)
			{
				if (clazz.getName().equals("javafx.scene.control.Tooltip$TooltipBehavior"))
				{
					Constructor ctor = clazz.getDeclaredConstructor(Duration.class, Duration.class, Duration.class, boolean.class);
					ctor.setAccessible(true);
					Object tooltipBehavior = ctor.newInstance(new Duration(openDelay), new Duration(visibleDuration), new Duration(closeDelay), false);
					f.set(null, tooltipBehavior);
					break;
				}
			}
		}
		catch (Exception e)	{} //if it fails, it fails, no big deal, no action we can take to fix.
	}
}