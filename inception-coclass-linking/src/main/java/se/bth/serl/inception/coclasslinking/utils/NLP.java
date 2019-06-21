/*
 * Copyright 2019
 * Software Engineering Research Lab
 * Blekinge Institute of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.bth.serl.inception.coclasslinking.utils;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.snowball.SnowballStemmer;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;

public class NLP {
	private static AnalysisEngine aEngine = null;
	
	public static AnalysisEngine baseAnalysisEngine() throws RecommendationException {
		if (aEngine == null) {
			try {
				AggregateBuilder builder = new AggregateBuilder();
				builder.add(createEngineDescription(OpenNlpSegmenter.class));
				builder.add(createEngineDescription(OpenNlpPosTagger.class));
				builder.add(createEngineDescription(SnowballStemmer.class));
				aEngine = builder.createAggregate();
			} catch (ResourceInitializationException e) {
				throw new RecommendationException("Could not create analysis engine.", e);
			}
		}
		
		return aEngine;
	}
}
