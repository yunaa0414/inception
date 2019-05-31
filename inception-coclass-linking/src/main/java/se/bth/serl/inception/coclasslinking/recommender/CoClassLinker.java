package se.bth.serl.inception.coclasslinking.recommender;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
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
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.wicket.util.file.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.snowball.SnowballStemmer;
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
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;

public class CoClassLinker implements RecommendationEngine {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Recommender recommender;
	private final String layerName;
    private final String featureName;
    private final KnowledgeBaseService kbService;
    private final FeatureSupportRegistry fsRegistry;
    private File modelPath;
    private Map<String, List<CCObject>> lookupTable;
    private AnalysisEngine anEngine;
    
    public CoClassLinker(Recommender aRecommender, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) {
    	recommender = aRecommender;
    	layerName = aRecommender.getLayer().getName();
    	featureName = aRecommender.getFeature().getName();
    	kbService = aKbService;
    	fsRegistry = aFsRegistry;
    	
    	
    	
    	Optional<File> mP = getModelPath();
    	if(mP.isPresent()) {
    		modelPath = mP.get();
    		
    		try {
        		anEngine = getAnalysisEngine();
        		lookupTable = getLookupTable();
        	} catch (RecommendationException e) {
        		log.error(e.getMessage(), e.getCause());
        	}
    	} else {
    		log.error("Could not determine CoClass model path");
    	}
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
			SimplePipeline.runPipeline(aCas, anEngine);
			
			Type predictionType = getAnnotationType(aCas, PredictedSpan.class);
			Feature labelFeature = predictionType.getFeatureByBaseName("label");
			Feature confidenceFeature = predictionType.getFeatureByBaseName("score");
			
			for (Token token : JCasUtil.select(aCas.getJCas(), Token.class)) {
				String posV = token.getPosValue();
				if (isNoun(posV)) {
					List<CCObject> hits = lookupTable.get(token.getStemValue().toLowerCase());
					if (hits != null) {
						for (CCObject hit : hits) {
							AnnotationFS annotation = aCas.createAnnotation(predictionType, token.getBegin(), token.getEnd());
							annotation.setDoubleValue(confidenceFeature, 0.0);
				            annotation.setStringValue(labelFeature, hit.getIri());
				            aCas.addFsToIndexes(annotation);
						}
					}
				}
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
	
	private AnalysisEngine getAnalysisEngine() throws RecommendationException {
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
								SimplePipeline.runPipeline(jcas, anEngine);
								
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
	
	private boolean isNoun(String posV) {
		return posV.equals("PN") || posV.equals("MN") || posV.equals("AN") || posV.equals("VN") || posV.equals("NN");
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
