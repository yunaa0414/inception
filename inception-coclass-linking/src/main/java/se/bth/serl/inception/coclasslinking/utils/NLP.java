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
