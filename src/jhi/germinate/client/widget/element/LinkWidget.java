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

package jhi.germinate.client.widget.element;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.*;

import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.*;

import java.util.*;

import jhi.germinate.client.i18n.*;
import jhi.germinate.client.service.*;
import jhi.germinate.client.util.*;
import jhi.germinate.client.util.callback.*;
import jhi.germinate.shared.*;
import jhi.germinate.shared.datastructure.*;
import jhi.germinate.shared.datastructure.database.*;
import jhi.germinate.shared.enums.*;

/**
 * @author Sebastian Raubach
 */
public class LinkWidget extends Composite
{
	private GerminateDatabaseTable referenceTable;
	private Long                   referenceId;

	private FlowPanel panel = new FlowPanel();

	public LinkWidget()
	{
		initWidget(panel);
	}

	public LinkWidget(GerminateDatabaseTable referenceTable, Long referenceId)
	{
		this.referenceTable = referenceTable;
		this.referenceId = referenceId;
	}

	public void update(GerminateDatabaseTable referenceTable, Long referenceId)
	{
		this.referenceTable = referenceTable;
		this.referenceId = referenceId;

		setUpContent();
	}

	protected void setUpContent()
	{
		if (referenceId != null && referenceTable != null)
		{
			CommonService.Inst.get().getExternalLinks(Cookie.getRequestProperties(), referenceId, referenceTable, new DefaultAsyncCallback<ServerResult<List<Link>>>()
			{
				@Override
				public void onSuccessImpl(ServerResult<List<Link>> result)
				{
					if (result.hasData())
					{
						ULPanel ulPanel = new ULPanel();

						for (Link link : result.getServerResult())
						{
							if (!StringUtils.isEmpty(link.getHyperlink()))
							{
								String description = link.getDescription();

								if (StringUtils.isEmpty(description))
									description = link.getType().getDescription();

								ulPanel.add(new Anchor(description, link.getHyperlink(), "_blank"));
							}
						}

						if (ulPanel.getWidgetCount() > 0)
						{
							panel.add(ulPanel);
							onDataChanged();
						}
						else
						{
							panel.add(new Heading(HeadingSize.H4, Text.LANG.notificationNoDataFound()));
						}
					}
					else
					{
						panel.add(new Heading(HeadingSize.H4, Text.LANG.notificationNoDataFound()));
					}
				}
			});
		}
	}

	protected void onDataChanged()
	{

	}
}
