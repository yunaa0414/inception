package se.bth.serl.inception.coclasslinking.predictor;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.wicket.util.file.File;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;

public class Word2VecPredictor extends PredictorBase {
	
	public Word2VecPredictor(File aModelPath, Recommender aRecommender, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) {
		super(aModelPath, aRecommender, aKbService, aFsRegistry);
	}
	
	@Override
	public void predict(RecommenderContext aContext, CAS aCas) throws AnalysisEngineProcessException, CASException {
		// TODO Auto-generated method stub

	}

}
