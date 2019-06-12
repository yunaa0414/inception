package se.bth.serl.inception.coclasslinking.predictor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.UIMAException;
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
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import se.bth.serl.inception.coclasslinking.recommender.CCObject;
import se.bth.serl.inception.coclasslinking.utils.NLP;

public abstract class PredictorBase implements IPredictor {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final File modelPath;
	protected final Recommender recommender;
	protected final KnowledgeBaseService kbService;
	protected final FeatureSupportRegistry fsRegistry;
	protected static Map<String, List<CCObject>> lookupTable = null;
	
	public PredictorBase(File aModelPath, Recommender aRecommender, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) {
		modelPath = aModelPath;
		recommender = aRecommender;
		kbService = aKbService;
		fsRegistry = aFsRegistry;
		try {
			if (lookupTable == null) {
				lookupTable = getLookupTable();
			}
    	} catch (RecommendationException e) {
    		log.error(e.getMessage(), e.getCause());
    	}
	}
	
	protected boolean isNoun(String posV) {
		return posV.equals("PN") || posV.equals("MN") || posV.equals("AN") || posV.equals("VN") || posV.equals("NN");
	}
	
	protected void logScore(Token token, Map<CCObject, Double> result) {
		StringBuffer msg = new StringBuffer();
		msg.append(token.getText().toLowerCase());
		result.forEach((k,v) -> {
			msg.append(" / " + k.getName() + " (" + v + ") ");
		});
		
		log.debug(msg.toString());
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
								SimplePipeline.runPipeline(jcas, NLP.baseAnalysisEngine());
								
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
