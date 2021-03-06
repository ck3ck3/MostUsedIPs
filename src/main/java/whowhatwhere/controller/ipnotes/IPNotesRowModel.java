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
package whowhatwhere.controller.ipnotes;

import javafx.beans.property.SimpleStringProperty;

public class IPNotesRowModel
{
	private SimpleStringProperty ipAddress;
	private SimpleStringProperty notes;
	
	public IPNotesRowModel(String ipAddress, String notes)
	{
		this.ipAddress = new SimpleStringProperty(ipAddress);
		this.notes = new SimpleStringProperty(notes);
	}
	
	public SimpleStringProperty ipAddressProperty()
	{
		return ipAddress;
	}
	
	public void setIpAddress(String ipAddress)
	{
		this.ipAddress.setValue(ipAddress);
	}

	public SimpleStringProperty notesProperty()
	{
		return notes;
	}
	
	public void setNotes(String notes)
	{
		this.notes.setValue(notes);
	}
}
