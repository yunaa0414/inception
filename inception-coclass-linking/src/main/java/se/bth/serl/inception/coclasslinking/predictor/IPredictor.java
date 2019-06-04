package se.bth.serl.inception.coclasslinking.predictor;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;

public interface IPredictor {
	public void predict(RecommenderContext aContext, CAS aCas) throws AnalysisEngineProcessException, CASException; 
}
