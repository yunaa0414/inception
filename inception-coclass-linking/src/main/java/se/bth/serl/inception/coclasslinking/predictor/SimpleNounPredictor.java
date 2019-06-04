package se.bth.serl.inception.coclasslinking.predictor;

import static org.apache.uima.fit.util.CasUtil.getAnnotationType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.wicket.util.file.File;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import se.bth.serl.inception.coclasslinking.recommender.CCObject;

public class SimpleNounPredictor extends PredictorBase {
	private Map<String, List<CCObject>> lookupTable;
	
	public SimpleNounPredictor(File aModelPath, Recommender aRecommender, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) {
		super(aModelPath, aRecommender, aKbService, aFsRegistry);
		try {
			lookupTable = getLookupTable();
		} catch (RecommendationException e) {
			log.error("Could not create lookup table.", e);
		}
	}
	
	@Override
	public void predict(RecommenderContext aContext, CAS aCas) throws AnalysisEngineProcessException, CASException {
		aCas.setDocumentLanguage("sv");
		SimplePipeline.runPipeline(aCas, aEngine);
		
		Type predictionType = getAnnotationType(aCas, PredictedSpan.class);
		Feature labelFeature = predictionType.getFeatureByBaseName("label");
		Feature confidenceFeature = predictionType.getFeatureByBaseName("score");
		
		/*
		 * Score components of coclass object:
		 * - frequency of term in coclass. The more frequent a term (appears in more coclass objects), the lower the score: 1 / total_term_occurrences
		 * - top X similar terms from word2vec and their score: (1 / total_similar_term_occurences) * similarity * 1 / X
		 * If no term was found in coclass, decompound term and do the above scoring on the components.
		 */
		
		
		for (Token token : JCasUtil.select(aCas.getJCas(), Token.class)) {
			String posV = token.getPosValue();
			if (isNoun(posV)) {
				List<CCObject> hits = lookupTable.get(token.getStemValue().toLowerCase());
				if (hits != null) {
					for (CCObject hit : hits) {
						AnnotationFS annotation = aCas.createAnnotation(predictionType, token.getBegin(), token.getEnd());
						annotation.setDoubleValue(confidenceFeature, 1.0 / hits.size());
			            annotation.setStringValue(labelFeature, hit.getIri());
			            aCas.addFsToIndexes(annotation);
					}
				}
			}
		}
	}
	
	private Map<String, List<CCObject>> getLookupTable() throws RecommendationException {
		Map<String, List<CCObject>> lookupTable = new HashMap<>();
		try {
			File modelFile = modelPath.file("coclass-lookup.bin");
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
			        FeatureSupport<ConceptFeatureTraits> fs = fsRegistry.getFeatureSupport(feat);
			        ConceptFeatureTraits conceptFeatureTraits = fs.readTraits(feat);

			        if (conceptFeatureTraits.getRepositoryId() != null) {
			            Optional<KnowledgeBase> kb = kbService.getKnowledgeBaseById(recommender.getProject(),
			                    conceptFeatureTraits.getRepositoryId());
			            if (kb.isPresent()) {
			            	List<KBHandle> handles = kbService.listAllConcepts(kb.get(), true);
			            	JCas jcas = JCasFactory.createJCas();
			            	for (int i = 0; i < handles.size(); i++) {
			            		KBHandle handle = handles.get(i);
			            		CCObject cc = new CCObject();
			            		cc.setIri(handle.getIdentifier());
								cc.setName(handle.getName());
								cc.setDefinition(handle.getDescription());
								
								List<KBStatement> sms = kbService.listStatements(kb.get(), handle, true);
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
								SimplePipeline.runPipeline(jcas, aEngine);
								
								for (Token token : JCasUtil.select(jcas, Token.class)) {
									String posV = token.getPosValue();
									if (isNoun(posV)) { 
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
}
