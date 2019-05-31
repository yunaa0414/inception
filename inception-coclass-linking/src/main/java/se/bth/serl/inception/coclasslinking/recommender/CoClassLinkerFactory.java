package se.bth.serl.inception.coclasslinking.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static java.util.Arrays.asList;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;

@Component
public class CoClassLinkerFactory extends RecommendationEngineFactoryImplBase<Void> {
	public static final String ID = "se.bth.serl.inception.coclasslinking.recommender.coclassrecommender";
	
	private @Autowired KnowledgeBaseService kbService;
	private @Autowired FeatureSupportRegistry fsRegistry;
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return "CoClass Recommender";
	}

	@Override
	public RecommendationEngine build(Recommender aRecommender) {
		return new CoClassLinker(aRecommender, kbService, fsRegistry);
	}

	@Override
	public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature) {
		if (aLayer == null || aFeature == null) {
            return false;
        }
        return asList(SINGLE_TOKEN, TOKENS).contains(aLayer.getAnchoringMode())
            && !aLayer.isCrossSentence() && SPAN_TYPE.equals(aLayer.getType())
            && CAS.TYPE_NAME_STRING.equals(aFeature.getType()) || aFeature.isVirtualFeature();
	}

	@Override
    public boolean isEvaluable() {
        return false;
    }
}
