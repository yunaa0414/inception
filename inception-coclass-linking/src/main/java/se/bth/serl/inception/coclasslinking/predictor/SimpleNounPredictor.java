package se.bth.serl.inception.coclasslinking.predictor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.file.File;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import se.bth.serl.inception.coclasslinking.recommender.CCObject;

public class SimpleNounPredictor extends PredictorBase {
	
	
	public SimpleNounPredictor(File aModelPath, Recommender aRecommender, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) {
		super(aModelPath, aRecommender, aKbService, aFsRegistry);
	}
	
	@Override
	public Map<CCObject, Double> score(Token token) { 
		Map<CCObject, Double> result = new HashMap<>();
		
		String posV = token.getPosValue();
		if (isNoun(posV)) {
			List<CCObject> hits = lookupTable.get(token.getStemValue().toLowerCase());
			if (hits != null) {
				int numberOfHits = hits.size();
				for (CCObject hit : hits) {
					result.put(hit, new Double(1.0 / numberOfHits));
				}
			}
			
			logScore(token, result);
		}
		
		return result;
	}
}
