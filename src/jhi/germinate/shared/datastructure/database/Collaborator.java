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

import java.sql.*;
import java.util.Date;
import java.util.*;

import jhi.germinate.server.database.*;
import jhi.germinate.server.database.query.*;
import jhi.germinate.server.database.query.parser.*;
import jhi.germinate.server.database.query.writer.*;
import jhi.germinate.server.manager.*;
import jhi.germinate.server.util.*;
import jhi.germinate.shared.*;
import jhi.germinate.shared.datastructure.*;
import jhi.germinate.shared.exception.*;

/**
 * @author Sebastian Raubach
 */
public class Collaborator extends DatabaseObject
{
	public static final String ID             = "collaborators.id";
	public static final String FIRST_NAME     = "collaborators.first_name";
	public static final String LAST_NAME      = "collaborators.last_name";
	public static final String EMAIL          = "collaborators.email";
	public static final String PHONE          = "collaborators.phone";
	public static final String INSTITUTION_ID = "collaborators.institution_id";
	public static final String CREATED_ON     = "collaborators.created_on";
	public static final String UPDATED_ON     = "collaborators.updated_on";

	private String      firstName;
	private String      lastName;
	private String      email;
	private String      phone;
	private Institution institution;
	private Long        createdOn;
	private Long        updatedOn;

	public Collaborator()
	{
	}

	public Collaborator(Long id)
	{
		super(id);
	}

	public String getFirstName()
	{
		return firstName;
	}

	public Collaborator setFirstName(String firstName)
	{
		this.firstName = firstName;
		return this;
	}

	public String getLastName()
	{
		return lastName;
	}

	public Collaborator setLastName(String lastName)
	{
		this.lastName = lastName;
		return this;
	}

	public String getEmail()
	{
		return email;
	}

	public Collaborator setEmail(String email)
	{
		this.email = email;
		return this;
	}

	public String getPhone()
	{
		return phone;
	}

	public Collaborator setPhone(String phone)
	{
		this.phone = phone;
		return this;
	}

	public Institution getInstitution()
	{
		return institution;
	}

	public Collaborator setInstitution(Institution institution)
	{
		this.institution = institution;
		return this;
	}

	public Long getCreatedOn()
	{
		return createdOn;
	}

	public Collaborator setCreatedOn(Date createdOn)
	{
		if (createdOn == null)
			this.createdOn = null;
		else
			this.createdOn = createdOn.getTime();
		return this;
	}

	public Collaborator setCreatedOn(Long createdOn)
	{
		this.createdOn = createdOn;
		return this;
	}

	public Long getUpdatedOn()
	{
		return updatedOn;
	}

	public Collaborator setUpdatedOn(Date updatedOn)
	{
		if (updatedOn == null)
			this.updatedOn = null;
		else
			this.updatedOn = updatedOn.getTime();
		return this;
	}

	public Collaborator setUpdatedOn(Long updatedOn)
	{
		this.updatedOn = updatedOn;
		return this;
	}

	@Override
	public String toString()
	{
		return "Collaborator{" +
				"firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", email='" + email + '\'' +
				", phone='" + phone + '\'' +
				", institution=" + institution +
				", createdOn=" + createdOn +
				", updatedOn=" + updatedOn +
				"} " + super.toString();
	}

	@Override
	@GwtIncompatible
	public DatabaseObjectParser<? extends DatabaseObject> getDefaultParser()
	{
		return Collaborator.Parser.Inst.get();
	}

	@GwtIncompatible
	public static class Parser extends DatabaseObjectParser<Collaborator>
	{
		private static DatabaseObjectCache<Institution> INSTITUTION_CACHE;

		private Parser()
		{
			INSTITUTION_CACHE = createCache(Institution.class, InstitutionManager.class);
		}

		@Override
		public Collaborator parse(DatabaseResult row, UserAuth user, boolean foreignsFromResultSet) throws DatabaseException
		{
			try
			{
				Long id = row.getLong(ID);

				if (id == null)
				{
					return null;
				}
				else
				{
					return new Collaborator(id)
							.setFirstName(row.getString(FIRST_NAME))
							.setLastName(row.getString(LAST_NAME))
							.setEmail(row.getString(EMAIL))
							.setPhone(row.getString(PHONE))
							.setInstitution(INSTITUTION_CACHE.get(user, row.getLong(INSTITUTION_ID), row, foreignsFromResultSet))
							.setCreatedOn(row.getTimestamp(CREATED_ON))
							.setUpdatedOn(row.getTimestamp(UPDATED_ON));
				}
			}
			catch (InsufficientPermissionsException e)
			{
				return null;
			}
		}

		public static final class Inst
		{
			public static Parser get()
			{
				return Parser.Inst.InstanceHolder.INSTANCE;
			}

			private static final class InstanceHolder
			{
				private static final Parser INSTANCE = new Parser();
			}
		}
	}

	@GwtIncompatible
	public static class Writer implements DatabaseObjectWriter<Collaborator>
	{
		@Override
		public void write(Database database, Collaborator object, boolean isUpdate) throws DatabaseException
		{
			ValueQuery query = new ValueQuery(database, "INSERT INTO `collaborators` (" + FIRST_NAME + ", " + LAST_NAME + ", " + EMAIL + ", " + PHONE + ", " + INSTITUTION_ID + ", " + CREATED_ON + ", " + UPDATED_ON + ") VALUES (?, ?, ?, ?, ?, ?, ?)")
					.setString(object.getFirstName())
					.setString(object.getLastName())
					.setString(object.getEmail())
					.setString(object.getPhone())
					.setLong(object.getInstitution() != null ? object.getInstitution().getId() : null);

			if (object.getCreatedOn() != null)
				query.setTimestamp(new Date(object.getCreatedOn()));
			else
				query.setNull(Types.TIMESTAMP);
			if (object.getUpdatedOn() != null)
				query.setTimestamp(new Date(object.getUpdatedOn()));
			else
				query.setNull(Types.TIMESTAMP);

			ServerResult<List<Long>> ids = query.execute(false);

			if (ids != null && !CollectionUtils.isEmpty(ids.getServerResult()))
				object.setId(ids.getServerResult().get(0));
		}

		public static final class Inst
		{
			public static Writer get()
			{
				return Writer.Inst.InstanceHolder.INSTANCE;
			}

			private static final class InstanceHolder
			{
				private static final Writer INSTANCE = new Writer();
			}
		}
	}
}
