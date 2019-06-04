package se.bth.serl.inception.coclasslinking.recommender;

import java.util.List;
import java.util.Optional;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.wicket.util.file.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import se.bth.serl.inception.coclasslinking.predictor.IPredictor;
import se.bth.serl.inception.coclasslinking.predictor.SimpleNounPredictor;

public class CoClassLinker implements RecommendationEngine {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final IPredictor predictor;
    private File modelPath;
    
    public CoClassLinker(Recommender aRecommender, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) {
    	Optional<File> mP = getModelPath();
    	if(mP.isPresent()) {
    		modelPath = mP.get();
    	} else {
    		log.error("Could not determine CoClass model path");
    	}
    	
    	predictor = new SimpleNounPredictor(modelPath, aRecommender, aKbService, aFsRegistry);
    }
	
	@Override
	public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException {
		// TODO Auto-generated method stub
		aContext.markAsReadyForPrediction();
	}

	@Override
	public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException {
		try {		
			predictor.predict(aContext, aCas);
		} catch (AnalysisEngineProcessException e) {
			throw new RecommendationException("Could not run analysis engine for prediction.", e);
		} catch (CASException e) {
			throw new RecommendationException("Could not iterate through tokes in prediction.", e);
		}
	}
	
	@Override
	public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter) throws RecommendationException {
		throw new UnsupportedOperationException("Evaluation not supported");
	}
	
	@Override
	public boolean requiresTraining() {
		return false;
	}
	
	private Optional<File> getModelPath() {
		File modelPath = null;
		String path = System.getProperty(SettingsUtil.getPropApplicationHome());
		if (path != null) {
			modelPath = new File(path + "/models");
			log.info("CoClass model path: {}", modelPath.getAbsolutePath());
		} else {
			log.error("Could not determine CoClass model path");
		}
		
		return Optional.ofNullable(modelPath);
	}
}
