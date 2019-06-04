package se.bth.serl.inception.coclasslinking.predictor;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.wicket.util.file.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.snowball.SnowballStemmer;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;

public abstract class PredictorBase implements IPredictor {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final File modelPath;
	protected final Recommender recommender;
	protected final KnowledgeBaseService kbService;
	protected final FeatureSupportRegistry fsRegistry;
	protected AnalysisEngine aEngine;
	
	public PredictorBase(File aModelPath, Recommender aRecommender, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) {
		modelPath = aModelPath;
		recommender = aRecommender;
		kbService = aKbService;
		fsRegistry = aFsRegistry;
		try {
    		aEngine = baseAnalysisEngine();
    		
    	} catch (RecommendationException e) {
    		log.error(e.getMessage(), e.getCause());
    	}
	}
	
	protected boolean isNoun(String posV) {
		return posV.equals("PN") || posV.equals("MN") || posV.equals("AN") || posV.equals("VN") || posV.equals("NN");
	}
	
	private AnalysisEngine baseAnalysisEngine() throws RecommendationException {
		AnalysisEngine ae = null;
		try {
			AggregateBuilder builder = new AggregateBuilder();
			builder.add(createEngineDescription(OpenNlpSegmenter.class));
			builder.add(createEngineDescription(OpenNlpPosTagger.class));
			builder.add(createEngineDescription(SnowballStemmer.class));
			ae = builder.createAggregate();
		} catch (ResourceInitializationException e) {
			throw new RecommendationException("Could not create analysis engine.", e);
		}
		
		return ae;
	}
	
}
