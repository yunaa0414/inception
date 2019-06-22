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

package se.bth.serl.inception.coclasslinking.recommender;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.uima.fit.util.CasUtil.getAnnotationType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.wicket.util.file.File;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import se.bth.serl.inception.coclasslinking.predictor.HistoryPredictor;
import se.bth.serl.inception.coclasslinking.predictor.IPredictor;
import se.bth.serl.inception.coclasslinking.predictor.SimpleNounPredictor;
import se.bth.serl.inception.coclasslinking.predictor.Word2VecPredictor;
import se.bth.serl.inception.coclasslinking.utils.NLP;

public class CoClassLinker implements RecommendationEngine {
	public static final Key<Map<String,CCMapping>> KEY_MODEL = new Key<>("ccMapping");
	private static final String LOOKUP_FILE = "coclass-lookup.bin";
	private static final String W2V_FILE = "word2vec-sv.bin";
	private static Map<String, List<CCObject>> coClassModel = null;
	private static Word2Vec w2vModel = null;
	private final Logger log = LoggerFactory.getLogger(getClass());
	private Recommender recommender;
	private List<IPredictor> predictors;
    
    
    public CoClassLinker(Recommender aRecommender, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) {
    	predictors = new ArrayList<>();
    	
    	Optional<File> mP = getModelPath();
    	if(mP.isPresent()) {
    		File modelPath = mP.get();
    		
    		try {
    			if (coClassModel == null ) {
    				coClassModel = getCoClassLookupTable(modelPath, aKbService, aFsRegistry);
    			}
    			
    			if (w2vModel == null) {
    				w2vModel = WordVectorSerializer.readWord2VecModel(modelPath.file(W2V_FILE));
    			}
    			
    			predictors.add(new SimpleNounPredictor(coClassModel));
        		predictors.add(new Word2VecPredictor(coClassModel, w2vModel));
        		predictors.add(new HistoryPredictor(coClassModel));
    		} catch (RecommendationException e) {
    			log.error(e.getMessage(), e.getCause());
    		}
    	} else {
    		log.error("Could not determine CoClass model path");
    	}
    	    	
    	recommender = aRecommender;
    }
	
	@Override
	public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException {
		aContext.put(KEY_MODEL, learn(aCasses));
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
				calculateScore(aContext, token).forEach((iri, score) -> {
					AnnotationFS annotation = aCas.createAnnotation(predictionType, token.getBegin(), token.getEnd());
					annotation.setDoubleValue(confidenceFeature, score);
					annotation.setStringValue(labelFeature, iri);
					aCas.addFsToIndexes(annotation);
				});
			}	
		} catch (AnalysisEngineProcessException e) {
			throw new RecommendationException("Could not run analysis engine for prediction.", e);
		} catch (CASException e) {
			throw new RecommendationException("Could not iterate through tokens in prediction.", e);
		}
	}
	
	@Override
	public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter) throws RecommendationException {
		throw new UnsupportedOperationException("Evaluation not supported");
	}
	
	@Override
	public boolean requiresTraining() {
		return true;
	}
	
	private Optional<File> getModelPath() {
		File modelPath = null;
		String path = System.getProperty(SettingsUtil.getPropApplicationHome());
		if (path != null) {
			modelPath = new File(path + "/models");
			log.debug("CoClass model path: {}", modelPath.getAbsolutePath());
		} else {
			log.error("Could not determine CoClass model path");
		}
		
		return Optional.ofNullable(modelPath);
	}
	
	private LinkedHashMap<String, Double> calculateScore(RecommenderContext aContext, Token aToken) {
		Map<String, Double> totalScore = new HashMap<>();
		Term term = new Term(aToken);
		
		// Ensure that the total score is in the range [0..1]
		double scalingFactor = 1.0 / predictors.size();
		
		if (term.isNoun()) {
			for (IPredictor predictor : predictors) {
				Map<String, Double> intermediateScore = predictor.score(aContext, term);
				intermediateScore.forEach((key, value) -> {
					totalScore.merge(key, value * scalingFactor, (score, increment) -> score += increment);
				});
			}
		}
		
		return totalScore.entrySet().stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.limit(Long.MAX_VALUE) //TODO Retrieve from settings and then set here.
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}
	
	private Map<String,CCMapping> learn(List<CAS> aCasses) {
		Type annotationType = CasUtil.getType(aCasses.get(0), recommender.getLayer().getName());
	    Feature labelFeature = annotationType.getFeatureByBaseName(recommender.getFeature().getName());
	    
	    Map<String, CCMapping> model = new HashMap<>();
	    for (CAS cas : aCasses) {
	    	for (AnnotationFS annotation : CasUtil.select(cas, annotationType)) {
	    		String iri = annotation.getFeatureValueAsString(labelFeature);
                if (isNotEmpty(iri)) {
                	String term = annotation.getCoveredText().toLowerCase();
                	
                	CCMapping ccm = model.get(term);
                	if (ccm != null) {
                		ccm.addEntry(iri);
                	} else {
                		ccm = new CCMapping(term, iri);
                		model.put(term, ccm);
                	}
                }
	    	}
	    }
		
		return model;
	}
	
	private Map<String, List<CCObject>> getCoClassLookupTable(File aModelPath, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) throws RecommendationException {
		Map<String, List<CCObject>> lookupTable = new HashMap<>();
		try {
			File modelFile = aModelPath.file(LOOKUP_FILE);
			if (modelFile.exists()) {
				log.info("Using existing coclass lookup file.");
				
				ObjectInputStream io = new ObjectInputStream(modelFile.inputStream());
				lookupTable = (Map<String, List<CCObject>>) io.readObject();
				io.close();
				return lookupTable;
			} else {
				log.info("Generating coclass lookup file...");			
				try {
			        AnnotationFeature feat = recommender.getFeature();
			        FeatureSupport<ConceptFeatureTraits> fs = aFsRegistry.getFeatureSupport(feat);
			        ConceptFeatureTraits conceptFeatureTraits = fs.readTraits(feat);

			        if (conceptFeatureTraits.getRepositoryId() != null) {
			            Optional<KnowledgeBase> kb = aKbService.getKnowledgeBaseById(recommender.getProject(),
			                    conceptFeatureTraits.getRepositoryId());
			            if (kb.isPresent()) {
			            	List<KBHandle> handles = aKbService.listAllConcepts(kb.get(), true);
			            	JCas jcas = JCasFactory.createJCas();
			            	for (int i = 0; i < handles.size(); i++) {
			            		KBHandle handle = handles.get(i);
			            		CCObject cc = new CCObject();
			            		cc.setIri(handle.getIdentifier());
								cc.setName(handle.getName());
								cc.setDefinition(handle.getDescription());
								
								List<KBStatement> sms = aKbService.listStatements(kb.get(), handle, true);
								for (KBStatement s : sms) {
									if (s.getProperty() != null && s.getProperty().getName() != null && s.getValue() != null) {
										if (s.getProperty().getName().equals("dimension")) {
											cc.setTable(s.getValue().toString());
										}
										
										if (s.getProperty().getName().equals("code")) {
											cc.setCode(s.getValue().toString());
										}
										
										if (s.getProperty().getName().equals("synonym")) {
											cc.addSynonym(s.getValue().toString());
										}
									}
								}
								
								jcas.setDocumentText(cc.getText());
								jcas.setDocumentLanguage("sv");
								SimplePipeline.runPipeline(jcas, NLP.baseAnalysisEngine());
								
								for (Token token : JCasUtil.select(jcas, Token.class)) {
									Term term = new Term(token);
									if (term.isNoun()) { 
										cc.addNoun(token.getStemValue().toLowerCase());
									}
								}
								
								jcas.reset();
								
								for (String noun : cc.getNouns()) {
									if (lookupTable.containsKey(noun)) {
										lookupTable.get(noun).add(cc);
									} else {
										List<CCObject> ccobjects = new ArrayList<>();
										ccobjects.add(cc);
										lookupTable.put(noun, ccobjects);
									}
								}
								
								log.info("Analyzed {}/{} coclass objects.", i + 1, handles.size());
			            	}
			            	
			            }
			        } else {
			            log.error("Knowledgebase for feature {} not found", feat.getName());
			        }
					
					ObjectOutputStream oo = new ObjectOutputStream(modelFile.outputStream());
					oo.writeObject(lookupTable);
					oo.close();
				} catch (IOException e) {
					throw new RecommendationException("Problem reading/writing coclass data.", e);
				} catch (ResourceInitializationException e) {
					throw new RecommendationException("Could not create coclass collection reader.", e);
				} catch (UIMAException e) {
					throw new RecommendationException("Could not run coclass model pipeline.", e);
				}
				
				return lookupTable;
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new RecommendationException("Could not read coclass lookup file.", e);
		}
	}
	
	public static class CCMapping {
		private String term;
		private Map<String,Integer> iris;
		
		public CCMapping(String t, String iri) {
			term = t;
			iris = new HashMap<>();
			addEntry(iri);
		}
		
		public void addEntry(String iri) {
			iris.merge(iri, 1, (oldVal, one) -> oldVal + one);
		}
		
		public Map<String,Integer> getEntries() {
			return iris;
		}
	}
}
