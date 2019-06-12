package se.bth.serl.inception.coclasslinking.predictor;

import java.util.Map;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import se.bth.serl.inception.coclasslinking.recommender.CCObject;

public interface IPredictor {
	public Map<CCObject, Double> score(Token token); 
}
