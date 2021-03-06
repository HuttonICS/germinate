/*
 *  Copyright 2017 Information and Computational Sciences,
 *  The James Hutton Institute.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jhi.germinate.shared.datastructure.database;

import com.google.gwt.core.shared.*;

import java.util.*;

import jhi.germinate.server.database.*;
import jhi.germinate.server.database.query.parser.*;
import jhi.germinate.shared.datastructure.*;
import jhi.germinate.shared.exception.*;

/**
 * @author Sebastian Raubach
 */
public class LicenseLog extends DatabaseObject
{
	public static final String ID          = "licenselogs.id";
	public static final String LICENSE_ID  = "licenselogs.license_id";
	public static final String USER_ID     = "licenselogs.user_id";
	public static final String ACCEPTED_ON = "licenselogs.accepted_on";

	private Long license;
	private Long user;
	private Long acceptedOn;

	public LicenseLog()
	{
	}

	public LicenseLog(Long id)
	{
		super(id);
	}

	public Long getLicense()
	{
		return license;
	}

	public LicenseLog setLicense(Long license)
	{
		this.license = license;
		return this;
	}

	public Long getUser()
	{
		return user;
	}

	public LicenseLog setUser(Long user)
	{
		this.user = user;
		return this;
	}

	public Long getAcceptedOn()
	{
		return acceptedOn;
	}

	public LicenseLog setAcceptedOn(Long acceptedOn)
	{
		this.acceptedOn = acceptedOn;
		return this;
	}

	public LicenseLog setAcceptedOn(Date acceptedOn)
	{
		if (acceptedOn == null)
			this.acceptedOn = null;
		else
			this.acceptedOn = acceptedOn.getTime();
		return this;
	}

	@Override
	public String toString()
	{
		return "LicenseLog{" +
				"license=" + license +
				", user=" + user +
				", acceptedOn=" + acceptedOn +
				"} " + super.toString();
	}

	@Override
	@GwtIncompatible
	public DatabaseObjectParser<? extends DatabaseObject> getDefaultParser()
	{
		return Parser.Inst.get();
	}

	@GwtIncompatible
	public static class Parser extends DatabaseObjectParser<LicenseLog>
	{
		public static final class Inst
		{
			/**
			 * {@link InstanceHolder} is loaded on the first execution of {@link Inst#get()} or the first access to {@link InstanceHolder#INSTANCE},
			 * not before. <p/> This solution (<a href= "http://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom" >Initialization-on-demand
			 * holder idiom</a>) is thread-safe without requiring special language constructs (i.e. <code>volatile</code> or
			 * <code>synchronized</code>).
			 *
			 * @author Sebastian Raubach
			 */
			private static final class InstanceHolder
			{
				private static final Parser INSTANCE = new Parser();
			}

			public static Parser get()
			{
				return InstanceHolder.INSTANCE;
			}
		}

		private Parser()
		{
		}

		@Override
		public LicenseLog parse(DatabaseResult row, UserAuth user, boolean foreignsFromResultSet) throws DatabaseException
		{
			Long id = row.getLong(ID);

			if (id == null)
				return null;
			else
			{
				return new LicenseLog(id)
						.setLicense(row.getLong(LICENSE_ID))
						.setUser(row.getLong(USER_ID))
						.setAcceptedOn(row.getTimestamp(ACCEPTED_ON));
			}
		}
	}
}
