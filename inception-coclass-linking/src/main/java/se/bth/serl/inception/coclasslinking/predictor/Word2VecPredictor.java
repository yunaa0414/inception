package se.bth.serl.inception.coclasslinking.predictor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.file.File;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import se.bth.serl.inception.coclasslinking.recommender.CCObject;

public class Word2VecPredictor extends PredictorBase {
	private static Word2Vec w2vModel = null;
	private final int numSimilarWords = 10;
	
	/*
	 * Using pre-trained model: https://github.com/Kyubyong/wordvectors
	 * In order to use it in D4J, I had to load it in python first:
	 * >>> from gensim.models import Word2Vec
	 * >>> model = Word2Vec.load('/home/mun/nosync/word2vec/kyubyong/sv.bin')
	 * and then save in a new file:
	 * https://stackoverflow.com/a/51293410/2091625
	 * 
	 * There are a couple of other larger pre-trained models:
	 * http://vectors.nlpl.eu/repository/
	 * https://fasttext.cc/docs/en/crawl-vectors.html
	 * 
	 * We can also train our own model with the wikipedia and the spearfishing corpus.
	 */
	public Word2VecPredictor(File aModelPath, Recommender aRecommender, KnowledgeBaseService aKbService, FeatureSupportRegistry aFsRegistry) {
		super(aModelPath, aRecommender, aKbService, aFsRegistry);
		if (w2vModel == null) {
			w2vModel = WordVectorSerializer.readWord2VecModel(modelPath.file("word2vec-sv.bin"));
		}
	}
	
	@Override
	public Map<CCObject, Double> score(Token token) {
		Map<CCObject, Double> result = new HashMap<>();
		
		String posV = token.getPosValue();
		if (isNoun(posV)) {
			String word = token.getText().toLowerCase();
			Collection<String> similarWords = w2vModel.wordsNearest(word, numSimilarWords);
			
			for (String similarWord : similarWords) {
				List<CCObject> hits = lookupTable.get(similarWord.toLowerCase());
				if (hits != null) {
					int numberOfHits = hits.size();
					for (CCObject hit : hits) {
						double newscore = 1.0 / numberOfHits * w2vModel.similarity(word, similarWord);
						result.merge(hit, newscore, (score, increment) -> score += increment);
					}
				}
			}
			
			logScore(token, result);
		}
		
		return result;
	}
}
