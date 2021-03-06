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

package jhi.germinate.client.page.dataset;

import com.google.gwt.core.client.*;
import com.google.gwt.uibinder.client.*;
import com.google.gwt.user.client.ui.*;

import jhi.germinate.client.i18n.*;
import jhi.germinate.client.page.*;
import jhi.germinate.client.util.*;
import jhi.germinate.client.util.parameterstore.*;
import jhi.germinate.client.widget.element.*;
import jhi.germinate.client.widget.structure.resource.*;
import jhi.germinate.shared.enums.*;
import jhi.germinate.shared.search.*;

/**
 * @author Sebastian Raubach
 */
public class DatasetPage extends Composite implements ParallaxBannerPage, HasHelp
{
	interface DatasetPageUiBinder extends UiBinder<HTMLPanel, DatasetPage>
	{
	}

	private static DatasetPageUiBinder ourUiBinder = GWT.create(DatasetPageUiBinder.class);

	@UiField
	HTML          internalText;
	@UiField
	DatasetWidget internalTable;
	@UiField
	HTML          externalText;
	@UiField
	DatasetWidget externalTable;

	public DatasetPage()
	{
		initWidget(ourUiBinder.createAndBindUi(this));

		internalText.setHTML(Text.LANG.datasetsTextInternal());
		externalText.setHTML(Text.LANG.datasetsTextExternal());

		/* Apply any filtering that another page requested before redirecting here */
		PartialSearchQuery query = FilterMappingParameterStore.Inst.get().get(Parameter.tableFilterMapping);
		FilterMappingParameterStore.Inst.get().remove(Parameter.tableFilterMapping);

		if (query != null)
		{
			Scheduler.get().scheduleDeferred(() -> {
				internalTable.getTable().forceFilter(query, true);
				externalTable.getTable().forceFilter(query, true);
			});
		}
	}

	@Override
	public String getParallaxStyle()
	{
		return ParallaxResource.INSTANCE.css().parallaxDataset();
	}

	@Override
	public Widget getHelpContent()
	{
		return new HTML(Text.LANG.datasetOverviewHelp());
	}
}