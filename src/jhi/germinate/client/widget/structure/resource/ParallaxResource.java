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

package jhi.germinate.client.widget.structure.resource;

import com.google.gwt.core.client.*;
import com.google.gwt.resources.client.*;

/**
 * @author Sebastian Raubach
 */
public interface ParallaxResource extends ClientBundle
{
	ParallaxResource INSTANCE = GWT.create(ParallaxResource.class);

	/* Make sure the resource is injected */
	Boolean IS_INJECTED = INSTANCE.css().ensureInjected();

	@Source("parallax.css")
	PagerResourceBundle css();

	interface PagerResourceBundle extends CssResource
	{
		String parallaxAboutGerminate();

		String parallaxAboutProject();

		String parallaxAccession();

		String parallaxAcknowledgements();

		String parallaxClimate();

		String parallaxDataset();

		String parallaxDataStats();

		String parallaxEnvironment();

		String parallaxGenotype();

		String parallaxGeneticMap();

		String parallaxGeographicSearch();

		String parallaxGroup();

		String parallaxGeography();

		String parallaxPassport();

		String parallaxPhenotype();

		String parallaxTrial();
	}
}