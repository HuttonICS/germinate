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

package jhi.germinate.shared.datastructure;

import java.io.*;
import java.util.*;

/**
 * @author Sebastian Raubach
 */
public class FlapjackProjectCreationResult implements Serializable
{
	private static final long serialVersionUID = 928555372594183865L;

	private String      debugOutput;
	private CreatedFile projectFile;
	private CreatedFile mapFile;
	private CreatedFile tabDelimitedFile;
	private CreatedFile rawDataFile;
	private Set<String> deletedMarkers;

	public FlapjackProjectCreationResult()
	{

	}

	public String getDebugOutput()
	{
		return debugOutput;
	}

	public FlapjackProjectCreationResult setDebugOutput(String debugOutput)
	{
		this.debugOutput = debugOutput;
		return this;
	}

	public CreatedFile getProjectFile()
	{
		return projectFile;
	}

	public FlapjackProjectCreationResult setProjectFile(CreatedFile projectFile)
	{
		this.projectFile = projectFile;
		return this;
	}

	public CreatedFile getMapFile()
	{
		return mapFile;
	}

	public FlapjackProjectCreationResult setMapFile(CreatedFile mapFile)
	{
		this.mapFile = mapFile;
		return this;
	}

	public CreatedFile getTabDelimitedFile()
	{
		return tabDelimitedFile;
	}

	public FlapjackProjectCreationResult setTabDelimitedFile(CreatedFile tabDelimitedFile)
	{
		this.tabDelimitedFile = tabDelimitedFile;
		return this;
	}

	public Set<String> getDeletedMarkers()
	{
		return deletedMarkers;
	}

	public FlapjackProjectCreationResult setDeletedMarkers(Set<String> deletedMarkers)
	{
		this.deletedMarkers = deletedMarkers;
		return this;
	}

	public CreatedFile getRawDataFile()
	{
		return rawDataFile;
	}

	public FlapjackProjectCreationResult setRawDataFile(CreatedFile rawDataFile)
	{
		this.rawDataFile = rawDataFile;
		return this;
	}
}
