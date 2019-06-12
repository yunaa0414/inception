package se.bth.serl.inception.coclasslinking.recommender;

import static org.apache.uima.fit.util.CasUtil.getAnnotationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.wicket.util.file.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import se.bth.serl.inception.coclasslinking.predictor.IPredictor;
import se.bth.serl.inception.coclasslinking.predictor.SimpleNounPredictor;
import se.bth.serl.inception.coclasslinking.predictor.Word2VecPredictor;
import se.bth.serl.inception.coclasslinking.utils.NLP;

public class CoClassLinker implements RecommendationEngine {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private List<IPredictor> predictors;
    private File modelPath;
    
    public CoClassLinker(Recommender aRecommender, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) {
    	Optional<File> mP = getModelPath();
    	if(mP.isPresent()) {
    		modelPath = mP.get();
    	} else {
    		log.error("Could not determine CoClass model path");
    	}
    	
    	predictors = new ArrayList<>();
    	predictors.add(new SimpleNounPredictor(modelPath, aRecommender, aKbService, aFsRegistry));
    	predictors.add(new Word2VecPredictor(modelPath, aRecommender, aKbService, aFsRegistry));
    }
	
	@Override
	public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException {
		// TODO Auto-generated method stub
		aContext.markAsReadyForPrediction();
	}

	@Override
	public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException {
		try {
			aCas.setDocumentLanguage("sv");
			SimplePipeline.runPipeline(aCas, NLP.baseAnalysisEngine());
			
			Type predictionType = getAnnotationType(aCas, PredictedSpan.class);
			Feature labelFeature = predictionType.getFeatureByBaseName("label");
			Feature confidenceFeature = predictionType.getFeatureByBaseName("score");
			
			for (Token token : JCasUtil.select(aCas.getJCas(), Token.class)) {	
				calculateScore(token).forEach((k, v) -> {
					AnnotationFS annotation = aCas.createAnnotation(predictionType, token.getBegin(), token.getEnd());
					annotation.setDoubleValue(confidenceFeature, v);
					annotation.setStringValue(labelFeature, k.getIri());
					aCas.addFsToIndexes(annotation);
				});
			}	
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
	
	private LinkedHashMap<CCObject, Double> calculateScore(Token token) {
		Map<CCObject, Double> totalScore = new HashMap<>();
		for (IPredictor predictor : predictors) {
			Map<CCObject, Double> intermediateScore = predictor.score(token);
			intermediateScore.forEach((key, value) -> {
				totalScore.merge(key, value, (score, increment) -> score += increment);
			});
		}
		
		//TODO
		// - check if results are actually ordered
		// - do debug logging that shows the individual components of the total score. Log in console. MAYBE add info to UI.
		
		/*
		 * Score components of coclass object:
		 * - frequency of term in coclass. The more frequent a term (appears in more coclass objects), the lower the score: 1 / total_term_occurrences
		 * - top X similar terms from word2vec and their score: (1 / total_similar_term_occurences) * similarity
		 * If no term was found in coclass, decompound term and do the above scoring on the components.
		 */
		
		return totalScore.entrySet().stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.limit(Long.MAX_VALUE) //TODO Retrieve from settings and then set here.
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}
}
